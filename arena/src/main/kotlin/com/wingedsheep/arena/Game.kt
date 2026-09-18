package com.wingedsheep.arena

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.hidden.OpponentModel
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost

/**
 * Plays one full game between two decks with the same [AiProfile] on both seats, driving
 * [ActionProcessor] directly. A trimmed copy of the AI module's test-only `TableGameRunner`
 * (that class lives under `ai/src/test`, so a main-source module cannot depend on it): the
 * same wedge detection, action cap and illegal-action recovery, without the arena bookkeeping.
 *
 * Mulligans are skipped, as in the engine's own arena, so a game is a pure function of
 * (decks, seed). Seat 0 is on the play.
 */
class GameRunner(
    private val registry: CardRegistry,
    private val profile: AiProfile,
    private val maxTurnsPerSeat: Int = 50,
    private val maxActions: Int = 20_000,
    /**
     * A game whose battlefield holds more permanents than this stops undecided (`board(n)`). Off by default; a
     * guard the replay's rollouts can opt into (`-Dreplay.rollMaxPermanents`) against a token or copy loop.
     */
    private val maxPermanents: Int = Int.MAX_VALUE,
    /** Print the stack trace of an exception that ends a game (the `one` mode). */
    private val printTraces: Boolean = false,
    /** Count [Outcome.holding] (mtg-draft-ai `docs/28` §8); off, a game is not enumerated twice. */
    private val measureHolding: Boolean = false,
    /** Count [Outcome.cards] (mtg-draft-ai `docs/33` §13); off, a game is not enumerated twice. */
    private val measureCards: Boolean = false,
    /** Optional learned policy for [policySeat]; complex pending decisions remain with [AIPlayer]. */
    private val gameplayPolicy: GameplayPolicyBridge? = null,
) {
    private val processor = ActionProcessor(registry)
    private val enumerator = LegalActionEnumerator.create(registry)
    private val initializer = GameInitializer(registry)

    class Outcome(
        val winnerSeat: Int?,
        val turns: Int,
        val actions: Int,
        val illegal: Int,
        val life: List<Int>,
        val reason: String,
        /**
         * Per seat, `[windows, passed, castTheInstant]`: priority on its own main phase with an empty stack and an
         * affordable instant in hand, and how often it passed or cast that instant there (anything else is the rest).
         * The engine's side of the own-games timing table (`own_games_timing.py`, `docs/32` §11). Null unless
         * [measureHolding].
         */
        val holding: List<List<Int>>? = null,
        /**
         * Per seat, mana over the turn cycle, summed over turns: `[ownTurnEnds, sources, untapped]` at the end of its
         * own turns, then the same at the end of the opponent's turns. Sources are [isManaSource]: lands, rocks and
         * mana creatures, not Treasures. Untapped at the end of the opponent's turn is wasted (it untaps next);
         * own-turn untapped minus that was spent on the opponent's turn.
         */
        val cycle: List<List<Int>>? = null,
        /**
         * Per seat, `[bigCasts, intoTappedOut]`: own-turn casts of mana value 4+, and how many came while the
         * opponent had at most one untapped mana source (the window a control deck resolves its big spell in).
         */
        val tappedOut: List<List<Int>>? = null,
        /** Per seat, casts by `own|opp:STEP:instant|creature|other`. */
        val casts: List<Map<String, Int>>? = null,
        /**
         * Per seat, per card name, the counts named by [CARD_FIELDS]: copies seen in hand; casts on its own turn
         * and on the opponent's; its own turns on which a copy in hand was castable in a main phase with an empty
         * stack, and how many of those turns ended with no copy cast; copies in hand when the game ended; the sum
         * of X over casts; casts whose player target was the caster, and the opponent. What the AI does with the
         * cards it is worst at (mtg-draft-ai `docs/33` §13). Null unless [measureCards].
         */
        val cards: List<Map<String, List<Int>>>? = null,
        /**
         * Per seat, the last sorcery-speed window ([AiProfile.spendIdleManaAtSorcerySpeed]), [LAST_WINDOW_FIELDS]:
         * own turns with a main-phase priority, and with a postcombat-main one; postcombat-main windows (empty
         * stack) with a sorcery-speed card castable from hand, and in those, passes, passes where no castable
         * sorcery-speed card fits beside the mana the cheapest instant-speed card in hand needs (the rule's
         * guard), sorcery-speed casts, and anything else. With [measureCards].
         */
        val lastWindow: List<List<Int>>? = null,
        /**
         * With `-Darena.gaps=true` as well: per seat and card name, how far below passing the AI scored each
         * sorcery-speed cast it passed over in its last sorcery-speed window, binned as [GAP_BINS] (the score
         * before any idle allowance; `dropped` = never scored).
         */
        val gaps: List<Map<String, List<Int>>>? = null,
        /** Learned-policy calls and combat declarations, for contract/legality diagnostics. */
        val policyActions: Int = 0,
        val policyPasses: Int = 0,
        val policyCasts: Int = 0,
        val policyLandPlays: Int = 0,
        val policyActivations: Int = 0,
        val policyOtherActions: Int = 0,
        val policyPaymentActions: Int = 0,
        val policyAttacks: Int = 0,
        val policyAttackers: Int = 0,
        val policyBlocks: Int = 0,
        val policyBlockers: Int = 0,
        val policyOrderMismatches: Int = 0,
        val policyFailures: Int = 0,
        val policyFirstFailure: String? = null,
        val policyIllegal: Int = 0,
        val policyFirstRejection: String? = null,
        /** [StrandedProbe], zeroed unless a `probeSeat` was given. */
        val probe: StrandedProbe = StrandedProbe(),
    )

    /**
     * Is the engine stranding cards it has the *amount* of mana for? (mtg-draft-ai `docs/33` §12.)
     *
     * Measured only at the probed seat's **own main phase with an empty stack**:
     *
     *  - [windows] — priority windows examined.
     *  - [affordable] — summed over windows: nonland cards in hand whose mana value the untapped lands cover.
     *  - [stranded] — of those, the ones the enumerator offers **no affordable cast for**.
     *  - [strandedCards] — distinct cards ever stranded.
     *  - [fixable] — **turns** in which, at some window with the land drop still available, a land in hand
     *    would have made a stranded card castable (each land is played in a copy of the state).
     *  - [fixMissed] — of those turns, the ones in which the AI never played any of those lands.
     *
     * Counted per turn, not per window: casting first and playing the fixing land later is not a mistake.
     * [fixMissed] is still a lower bound, since a turn whose first window came after a wrong land drop has no
     * drop left to check.
     */
    data class StrandedProbe(
        val windows: Int = 0,
        val affordable: Int = 0,
        val stranded: Int = 0,
        val strandedCards: Int = 0,
        val fixable: Int = 0,
        val fixMissed: Int = 0,
    )

    /** [seatProfiles] overrides [profile] seat by seat (a one-sided A/B); null plays [profile] on both. */
    fun play(
        decks: List<List<String>>,
        seed: Long,
        seatProfiles: List<AiProfile>? = null,
        /** Seat to run [StrandedProbe] for; null (the default) costs nothing at all. */
        probeSeat: Int? = null,
        /** Seat controlled by [gameplayPolicy] for priority actions and declarations. */
        policySeat: Int? = null,
    ): Outcome {
        val init = initializer.initializeGame(
            GameConfig(
                players = decks.mapIndexed { seat, deck -> PlayerConfig("Seat$seat", Deck(deck)) },
                skipMulligans = true,
                startingPlayerIndex = 0,
                seed = seed,
            )
        )
        val seatIds = init.state.turnOrder
        val decklists = seatIds.mapIndexed { seat, id ->
            id to OpponentModel.KnownDecklist(decks[seat].groupingBy { it }.eachCount())
        }.toMap()
        return playFrom(
            init.state, seatIds.indices.map { seatProfiles?.get(it) ?: profile }, decklists,
            probeSeat = probeSeat, policySeat = policySeat,
        )
    }

    /**
     * Plays [start] to the end with [seatProfiles] seat by seat, in `turnOrder`; [decklists] are the decks each
     * AI's opponent model may assume. What [play] does after dealing, and what the replay module plays on from a
     * rebuilt game's break with (mtg-draft-ai `docs/27` §5, C1). Seats in the [Outcome] are `turnOrder` indices.
     */
    fun playFrom(
        start: GameState,
        seatProfiles: List<AiProfile>,
        decklists: Map<EntityId, OpponentModel>,
        probeSeat: Int? = null,
        policySeat: Int? = null,
    ): Outcome {
        val seatIds = start.turnOrder
        val bySeat = seatIds.withIndex().associate { (seat, id) -> id to seat }
        val gaps = if (measureCards && System.getProperty("arena.gaps").toBoolean()) {
            seatIds.map { HashMap<String, IntArray>() }
        } else null
        val players = seatIds.mapIndexed { seat, id ->
            val sink = gaps?.let { g ->
                AiInsightSink { s, insight ->
                    if (!insight.onOwnTurn || s.step != Step.POSTCOMBAT_MAIN || s.stack.isNotEmpty()) return@AiInsightSink
                    if (insight.options.none { it.baseline && it.chosen }) return@AiInsightSink
                    for (o in insight.options) {
                        val cast = o.action as? CastSpell ?: continue
                        if (cast.cardId !in s.getHand(id) || isInstantSpeed(s, cast.cardId)) continue
                        val bin = o.advantage?.let { a -> GAP_EDGES.indexOfFirst { a < it }.let { if (it < 0) GAP_EDGES.size else it } }
                            ?: (GAP_BINS.size - 1)
                        g[seat].getOrPut(o.cardName ?: "?") { IntArray(GAP_BINS.size) }[bin]++
                    }
                }
            }
            AIPlayer.create(registry, id, seatProfiles[seat], decklists, insightSink = sink)
        }
        fun aiFor(playerId: EntityId) = players[bySeat.getValue(playerId)]

        var state: GameState = start
        var actionCount = 0
        var illegal = 0
        var policyActions = 0
        var policyPasses = 0
        var policyCasts = 0
        var policyLandPlays = 0
        var policyActivations = 0
        var policyOtherActions = 0
        var policyPaymentActions = 0
        var policyAttacks = 0
        var policyAttackers = 0
        var policyBlocks = 0
        var policyBlockers = 0
        var policyOrderMismatches = 0
        var policyFailures = 0
        var policyFirstFailure: String? = null
        var policyIllegal = 0
        var policyFirstRejection: String? = null
        var lastActivePlayer: EntityId? = null
        var lastProgressAction = 0
        var reason = ""
        val holding = if (measureHolding) seatIds.map { IntArray(3) } else null
        val cycle = if (measureHolding) seatIds.map { IntArray(6) } else null
        val casts = if (measureHolding) seatIds.map { mutableMapOf<String, Int>() } else null
        val tappedOut = if (measureHolding) seatIds.map { IntArray(2) } else null
        val cards = if (measureCards) seatIds.map { HashMap<String, IntArray>() } else null
        val seen = seatIds.map { HashSet<EntityId>() }
        // This turn's names castable in the active seat's main phase, and the names it cast this turn.
        val castableNow = HashSet<String>()
        val castNow = HashSet<String>()
        var cardsTurn = -1
        var cardsSeat = -1
        val lastWindow = if (measureCards) seatIds.map { IntArray(LAST_WINDOW_FIELDS.size) } else null
        var sawMain = false
        var sawPostcombat = false
        fun stat(seat: Int, name: String) = cards!![seat].getOrPut(name) { IntArray(CARD_FIELDS.size) }
        fun nameOf(s: GameState, id: EntityId) = s.getEntity(id)?.get<CardComponent>()?.name
        fun commitTurn() {
            for (name in castableNow) {
                val c = stat(cardsSeat, name)
                c[CASTABLE_TURNS]++
                if (name !in castNow) c[CASTABLE_NOT_CAST]++
            }
            castableNow.clear(); castNow.clear()
        }
        // Each seat's lands and untapped lands at the last step seen, committed when the turn passes.
        val lastLands = IntArray(seatIds.size)
        val lastUntapped = IntArray(seatIds.size)
        var cycleActive: EntityId? = null
        var probeWindows = 0
        var probeAffordable = 0
        var probeStranded = 0
        var probeFixable = 0
        var probeFixMissed = 0
        val probeStrandedIds = mutableSetOf<EntityId>()
        val probeId = probeSeat?.let { seatIds[it] }
        // Per-turn accounting for fixable / fixMissed; see StrandedProbe.
        var turnFixing = mutableSetOf<EntityId>()
        var turnLandPlayed: EntityId? = null
        var probeTurn = -1
        fun flushProbeTurn() {
            if (turnFixing.isNotEmpty()) {
                probeFixable++
                if (turnLandPlayed == null || turnLandPlayed !in turnFixing) probeFixMissed++
            }
            turnFixing = mutableSetOf()
            turnLandPlayed = null
        }
        val maxPlayerTurns = maxTurnsPerSeat * seatIds.size
        try {
            while (!state.gameOver && state.turnNumber < maxPlayerTurns && actionCount < maxActions) {
                if (maxPermanents < Int.MAX_VALUE) {
                    val permanents = state.allBattlefieldEntities().size
                    if (permanents > maxPermanents) {
                        reason = "board($permanents)"
                        break
                    }
                }
                if (actionCount - lastProgressAction > STUCK_ACTIONS_PER_TURN) {
                    reason = "stuck(turn=${state.turnNumber},step=${state.step.name})"
                    break
                }
                if (state.activePlayerId != lastActivePlayer) {
                    lastActivePlayer = state.activePlayerId
                    lastProgressAction = actionCount
                }
                if (cards != null) {
                    if (state.turnNumber != cardsTurn) {
                        if (cardsTurn >= 0) commitTurn()
                        if (cardsSeat >= 0) {
                            if (sawMain) lastWindow!![cardsSeat][0]++
                            if (sawPostcombat) lastWindow!![cardsSeat][1]++
                        }
                        sawMain = false; sawPostcombat = false
                        cardsTurn = state.turnNumber
                        cardsSeat = bySeat[state.activePlayerId] ?: -1
                    }
                    for ((seat, id) in seatIds.withIndex()) for (card in state.getHand(id)) {
                        if (seen[seat].add(card)) nameOf(state, card)?.let { stat(seat, it)[SEEN]++ }
                    }
                }
                if (cycle != null) {
                    val active = state.activePlayerId
                    if (cycleActive != null && active != cycleActive) {
                        for ((seat, id) in seatIds.withIndex()) {
                            val o = if (id == cycleActive) 0 else 3
                            cycle[seat][o]++; cycle[seat][o + 1] += lastLands[seat]; cycle[seat][o + 2] += lastUntapped[seat]
                        }
                    }
                    cycleActive = active
                    for ((seat, id) in seatIds.withIndex()) {
                        val sources = state.projectedState.getBattlefieldControlledBy(id).filter { isManaSource(state, it) }
                        lastLands[seat] = sources.size
                        lastUntapped[seat] = sources.count { state.getEntity(it)?.has<TappedComponent>() != true }
                    }
                }
                val decision = state.pendingDecision
                if (decision != null) {
                    actionCount++
                    val response = aiFor(decision.playerId).respondToDecision(state, decision)
                    val r = processor.process(state, SubmitDecision(decision.playerId, response)).result
                    if (r.error != null) {
                        reason = "decisionError(${r.error})"
                        break
                    }
                    state = r.state
                    continue
                }
                val priorityPlayer = state.priorityPlayerId
                if (priorityPlayer == null) {
                    reason = "noPriority(turn=${state.turnNumber})"
                    break
                }
                if (probeId != null && state.turnNumber != probeTurn) {
                    flushProbeTurn()
                    probeTurn = state.turnNumber
                }
                if (probeId != null && priorityPlayer == probeId &&
                    state.activePlayerId == probeId && state.stack.isEmpty() &&
                    state.phase in MAIN_PHASES
                ) {
                    val projected = state.projectedState
                    val untappedLands = projected.getBattlefieldControlledBy(probeId).count {
                        projected.hasType(it, "LAND") && state.getEntity(it)?.has<TappedComponent>() != true
                    }
                    val hand = state.getZone(probeId, Zone.HAND)
                    val castableIds = enumerator.enumerate(state, probeId, EnumerationMode.ACTIONS_ONLY)
                        .filter { it.affordable }
                        .mapNotNull { (it.action as? CastSpell)?.cardId }
                        .toSet()
                    var windowCounted = false
                    val strandedHere = mutableSetOf<EntityId>()
                    for (cardId in hand) {
                        val card = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                        if (card.isLand || card.manaValue > untappedLands) continue
                        probeAffordable++
                        windowCounted = true
                        if (cardId !in castableIds) {
                            probeStranded++
                            probeStrandedIds += cardId
                            strandedHere += cardId
                        }
                    }
                    if (windowCounted) probeWindows++
                    // Could a land in hand have cast one of them? Only asked when something is stranded.
                    if (strandedHere.isNotEmpty()) {
                        val landDrops = enumerator.enumerate(state, probeId, EnumerationMode.ACTIONS_ONLY)
                            .filter { it.actionType == "PlayLand" }
                        val fixing = landDrops.filter { drop ->
                            val after = processor.process(state, drop.action).result
                            after.error == null && enumerator
                                .enumerate(after.state, probeId, EnumerationMode.ACTIONS_ONLY)
                                .any { it.affordable && (it.action as? CastSpell)?.cardId in strandedHere }
                        }.mapNotNull { (it.action as? PlayLand)?.cardId }.toSet()
                        turnFixing += fixing
                    }
                }
                actionCount++
                val instants = if (holding != null) affordableInstants(state, priorityPlayer) else emptySet()
                val castable = if (cards != null) affordableCasts(state, priorityPlayer) else emptySet()
                for (card in castable) if (card in state.getHand(priorityPlayer)) nameOf(state, card)?.let(castableNow::add)
                val ownMain = cards != null && state.activePlayerId == priorityPlayer && state.step.isMainPhase
                if (ownMain) sawMain = true
                val sorcerySpeed = castable.filter { it in state.getHand(priorityPlayer) && !isInstantSpeed(state, it) }
                val atLastWindow = ownMain && state.step == Step.POSTCOMBAT_MAIN
                if (atLastWindow) sawPostcombat = true
                val policyChoice = if (bySeat[priorityPlayer] == policySeat) gameplayPolicy?.let { policy ->
                    runCatching { policy.choose(state, priorityPlayer) }.fold(
                        onSuccess = { chosen ->
                            if (!chosen.modelDecision) {
                                policyPaymentActions++
                            } else when (chosen.action) {
                                is PassPriority -> {
                                    policyActions++
                                    policyPasses++
                                }
                                is CastSpell -> {
                                    policyActions++
                                    policyCasts++
                                }
                                is PlayLand -> {
                                    policyActions++
                                    policyLandPlays++
                                }
                                is ActivateAbility -> {
                                    policyActions++
                                    policyActivations++
                                }
                                is DeclareAttackers -> {
                                    policyActions++
                                    policyAttacks++
                                    policyAttackers += chosen.declarationSize
                                }
                                is DeclareBlockers -> {
                                    policyActions++
                                    policyBlocks++
                                    policyBlockers += chosen.declarationSize
                                }
                                else -> {
                                    policyActions++
                                    policyOtherActions++
                                }
                            }
                            if (!chosen.combatOrderMatches) {
                                policyOrderMismatches++
                                error(
                                    "gameplay policy combat_order does not match ${chosen.kind} " +
                                        "(${chosen.combatOrderSize} ordered pairs, ${chosen.declarationSize} submitted)"
                                )
                            }
                            chosen
                        },
                        onFailure = { failure ->
                            policyFailures++
                            if (policyFirstFailure == null) {
                                policyFirstFailure = "${failure::class.simpleName}: ${failure.message}"
                            }
                            null
                        },
                    )
                } else null
                val action = policyChoice?.action ?: aiFor(priorityPlayer).chooseAction(state)
                if (probeId != null && priorityPlayer == probeId && action is PlayLand && turnLandPlayed == null) {
                    turnLandPlayed = action.cardId
                }
                if (atLastWindow && sorcerySpeed.isNotEmpty()) {
                    val w = lastWindow!![bySeat.getValue(priorityPlayer)]
                    w[2]++
                    when {
                        action is PassPriority -> {
                            w[3]++
                            if (sorcerySpeed.none { manaValue(state, it) <= spareMana(state, priorityPlayer) }) w[4]++
                        }
                        action is CastSpell && action.cardId in sorcerySpeed -> w[5]++
                        else -> w[6]++
                    }
                }
                if (cards != null && action is CastSpell) nameOf(state, action.cardId)?.let { name ->
                    val c = stat(bySeat.getValue(priorityPlayer), name)
                    if (state.activePlayerId == priorityPlayer) { c[CAST_OWN]++; castNow += name } else c[CAST_OPP]++
                    c[X_SUM] += action.xValue ?: 0
                    for (t in action.targets) if (t is ChosenTarget.Player) {
                        if (t.playerId == priorityPlayer) c[TARGET_SELF]++ else c[TARGET_OPPONENT]++
                    }
                }
                if (casts != null && action is CastSpell) {
                    val type = state.getEntity(action.cardId)?.get<CardComponent>()?.typeLine
                    val kind = when { type?.isInstant == true -> "instant"; type?.isCreature == true -> "creature"; else -> "other" }
                    val whose = if (state.activePlayerId == priorityPlayer) "own" else "opp"
                    casts[bySeat.getValue(priorityPlayer)].merge("$whose:${state.step.name}:$kind", 1, Int::plus)
                    val manaValue = state.getEntity(action.cardId)?.get<CardComponent>()?.manaValue ?: 0
                    if (whose == "own" && manaValue >= 4) {
                        val t = tappedOut!![bySeat.getValue(priorityPlayer)]
                        t[0]++
                        val opponentOpen = state.getOpponents(priorityPlayer).sumOf { o ->
                            state.projectedState.getBattlefieldControlledBy(o)
                                .count { isManaSource(state, it) && state.getEntity(it)?.has<TappedComponent>() != true }
                        }
                        if (opponentOpen <= 1) t[1]++
                    }
                }
                if (instants.isNotEmpty()) {
                    val h = holding!![bySeat.getValue(priorityPlayer)]
                    h[0]++
                    if (action is PassPriority) h[1]++ else if (action is CastSpell && action.cardId in instants) h[2]++
                }
                val r = processor.process(state, action).result
                val next = if (r.error != null) {
                    illegal++
                    if (policyChoice != null) {
                        policyIllegal++
                        if (policyFirstRejection == null) {
                            policyFirstRejection = "${r.error}; ${policyChoice.diagnostic}"
                        }
                    }
                    val fallback = processor.process(state, safeFallbackAction(state, priorityPlayer)).result
                    if (fallback.error != null) {
                        reason = "error(${r.error}; fallback: ${fallback.error})"
                        null
                    } else fallback.state
                } else r.state
                if (next == null) break
                if (next === state) {
                    reason = "noProgress(turn=${state.turnNumber},step=${state.step.name})"
                    break
                }
                state = next
            }
            if (!state.gameOver && reason.isEmpty()) {
                reason = if (actionCount >= maxActions) "maxActions($maxActions)" else "maxTurns($maxTurnsPerSeat)"
            }
        } catch (e: Throwable) {
            if (printTraces) e.printStackTrace()
            reason = "exception(${e::class.simpleName}: ${e.message?.take(200)})"
        }
        if (probeId != null) flushProbeTurn()
        if (cards != null) {
            if (cardsSeat >= 0) {
                commitTurn()
                if (sawMain) lastWindow!![cardsSeat][0]++
                if (sawPostcombat) lastWindow!![cardsSeat][1]++
            }
            for ((seat, id) in seatIds.withIndex()) for (card in state.getHand(id)) {
                nameOf(state, card)?.let { stat(seat, it)[END_IN_HAND]++ }
            }
        }
        val winnerSeat = if (state.gameOver) state.winnerId?.let { bySeat[it] } else null
        return Outcome(
            winnerSeat, state.turnNumber, actionCount, illegal, seatIds.map { state.lifeTotal(it) }, reason,
            holding = holding?.map { it.toList() }, cycle = cycle?.map { it.toList() }, casts = casts,
            tappedOut = tappedOut?.map { it.toList() },
            cards = cards?.map { seat -> seat.mapValues { it.value.toList() } },
            lastWindow = lastWindow?.map { it.toList() },
            gaps = gaps?.map { seat -> seat.mapValues { it.value.toList() } },
            policyActions = policyActions,
            policyPasses = policyPasses,
            policyCasts = policyCasts,
            policyLandPlays = policyLandPlays,
            policyActivations = policyActivations,
            policyOtherActions = policyOtherActions,
            policyPaymentActions = policyPaymentActions,
            policyAttacks = policyAttacks,
            policyAttackers = policyAttackers,
            policyBlocks = policyBlocks,
            policyBlockers = policyBlockers,
            policyOrderMismatches = policyOrderMismatches,
            policyFailures = policyFailures,
            policyFirstFailure = policyFirstFailure,
            policyIllegal = policyIllegal,
            policyFirstRejection = policyFirstRejection,
            probe = StrandedProbe(
                probeWindows, probeAffordable, probeStranded, probeStrandedIds.size, probeFixable, probeFixMissed,
            ),
        )
    }

    private val repeatableMana = HashMap<String, Boolean>()

    /**
     * A permanent that makes mana every turn: a land, or one with a mana ability whose cost doesn't sacrifice it (a
     * rock, a mana creature), and not a creature that can't tap yet. A Treasure is stored mana, cashed in when wanted,
     * so leaving it is not waste (the user's point, mtg-draft-ai `docs/28` §8.1).
     */
    private fun isManaSource(state: GameState, id: EntityId): Boolean {
        if (state.projectedState.hasType(id, "LAND")) return true
        val entity = state.getEntity(id) ?: return false
        val card = entity.get<CardComponent>() ?: return false
        val repeatable = repeatableMana.getOrPut(card.cardDefinitionId) {
            registry.getCard(card.cardDefinitionId)?.script?.activatedAbilities.orEmpty().any { ability ->
                ability.isManaAbility && ability.cost.let { c ->
                    c !is AbilityCost.SacrificeSelf && (c !is AbilityCost.Composite || c.costs.none { it is AbilityCost.SacrificeSelf })
                }
            }
        }
        return repeatable && !(state.projectedState.hasType(id, "CREATURE") && entity.has<SummoningSicknessComponent>())
    }

    /** The instants [playerId] could cast now, when it is their own main phase with an empty stack; else none. */
    private fun affordableInstants(state: GameState, playerId: EntityId): Set<EntityId> =
        affordableCasts(state, playerId).filterTo(HashSet()) {
            state.getEntity(it)?.get<CardComponent>()?.typeLine?.isInstant == true
        }

    private fun manaValue(state: GameState, id: EntityId): Int = state.getEntity(id)?.get<CardComponent>()?.manaValue ?: 0

    /** `Strategist`'s spare mana: untapped lands less the cheapest instant-speed card in hand they could pay for. */
    private fun spareMana(state: GameState, playerId: EntityId): Int {
        val untapped = state.projectedState.getBattlefieldControlledBy(playerId).count { id ->
            state.projectedState.hasType(id, "LAND") && state.getEntity(id)?.has<TappedComponent>() != true
        }
        val held = state.getHand(playerId).filter { isInstantSpeed(state, it) }.map { manaValue(state, it) }
            .filter { it <= untapped }.minOrNull() ?: 0
        return untapped - held
    }

    /** As `Strategist`'s rule reads it: an instant, or a nonland card with flash. */
    private fun isInstantSpeed(state: GameState, id: EntityId): Boolean {
        val card = state.getEntity(id)?.get<CardComponent>() ?: return false
        return !card.typeLine.isLand && (card.typeLine.isInstant || Keyword.FLASH in card.baseKeywords)
    }

    /** The cards [playerId] could cast now, when it is their own main phase with an empty stack; else none. */
    private fun affordableCasts(state: GameState, playerId: EntityId): Set<EntityId> {
        if (state.activePlayerId != playerId || !state.step.isMainPhase || state.stack.isNotEmpty()) return emptySet()
        return enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY).mapNotNull { la ->
            (la.action as? CastSpell)?.cardId?.takeIf { la.affordable }
        }.toSet()
    }

    /** Copy of the AI test support's fallback: pass, unless a combat declaration is owed. */
    private fun safeFallbackAction(state: GameState, playerId: EntityId): GameAction {
        val attackersDeclared = state.getEntity(playerId)?.has<AttackersDeclaredThisCombatComponent>() == true
        val blockersDeclared = state.getEntity(playerId)?.has<BlockersDeclaredThisCombatComponent>() == true
        return when {
            state.step == Step.DECLARE_ATTACKERS && state.activePlayerId == playerId && !attackersDeclared -> {
                val la = enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY)
                    .find { it.actionType == "DeclareAttackers" }
                val mandatory = la?.mandatoryAttackers ?: emptyList()
                val opponentId = state.getOpponents(playerId).firstOrNull()
                DeclareAttackers(
                    playerId,
                    if (mandatory.isNotEmpty() && opponentId != null) mandatory.associateWith { opponentId } else emptyMap(),
                )
            }
            state.step == Step.DECLARE_BLOCKERS && state.activePlayerId != playerId && !blockersDeclared -> {
                val la = enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY)
                    .find { it.actionType == "DeclareBlockers" }
                val mandatory = la?.mandatoryBlockerAssignments ?: emptyMap()
                DeclareBlockers(playerId, mandatory.mapValues { (_, targets) -> targets.take(1) })
            }
            else -> PassPriority(playerId)
        }
    }

    companion object {
        const val STUCK_ACTIONS_PER_TURN = 300
        private val MAIN_PHASES = setOf(Phase.PRECOMBAT_MAIN, Phase.POSTCOMBAT_MAIN)

        /** [Outcome.cards]' fields, in order. */
        val CARD_FIELDS = listOf(
            "seen", "cast_own", "cast_opp", "castable_turns", "castable_not_cast", "end_in_hand", "x_sum",
            "target_self", "target_opponent",
        )
        /** [Outcome.lastWindow]'s fields, in order. */
        val LAST_WINDOW_FIELDS = listOf(
            "turns_with_main", "turns_with_postcombat_main", "windows_with_sorcery_castable", "passed",
            "passed_guard_off", "cast_sorcery_speed", "other",
        )
        private val GAP_EDGES = listOf(-10.0, -5.0, -3.0, -1.0, 0.0)

        /** [Outcome.gaps]' bins: advantage over passing below −10, −10…−5, −5…−3, −3…−1, −1…0, ≥ 0, dropped. */
        val GAP_BINS = listOf("lt-10", "-10to-5", "-5to-3", "-3to-1", "-1to0", "ge0", "dropped")
        private const val SEEN = 0
        private const val CAST_OWN = 1
        private const val CAST_OPP = 2
        private const val CASTABLE_TURNS = 3
        private const val CASTABLE_NOT_CAST = 4
        private const val END_IN_HAND = 5
        private const val X_SUM = 6
        private const val TARGET_SELF = 7
        private const val TARGET_OPPONENT = 8
    }
}
