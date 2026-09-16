package com.wingedsheep.arena

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.hidden.OpponentModel
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId

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
    /** Print the stack trace of an exception that ends a game (the `one` mode). */
    private val printTraces: Boolean = false,
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
        /** [StrandedProbe], zeroed unless a `probeSeat` was given. */
        val probe: StrandedProbe = StrandedProbe(),
    )

    /**
     * Is the engine stranding cards it has the *amount* of mana for?
     *
     * mtg-draft-ai `docs/33` §9 ruled out the mana base **as built** — the three-colour deficit is flat
     * across the weight of the least-used colour and across how many sources that colour has — but that
     * is a fact about the decklist, not about how the engine plays it. No profile the pilots use sets
     * `AiProfile.sequenceLandsByUsableMana`, so these agents have no land-choice term at all beyond
     * tapped 0.3 / untapped 0.6; and that term, where it is on, says in its own KDoc that "colours,
     * alternative costs and timing are ignored". For a two-colour deck that barely matters. For a
     * three-colour deck it is the decision.
     *
     * Measured only at the probed seat's **own main phase with an empty stack**, so sorcery timing is
     * legal and an uncastable card is not merely a card being held for the right moment:
     *
     *  - [windows] — priority windows examined.
     *  - [affordable] — summed over windows: nonland cards in hand whose mana value is covered by the
     *    controller's untapped lands. Lands only, the same approximation `BoardPresence.landSequencing`
     *    makes, and for its reason: this runs per window and may not call a solver.
     *  - [stranded] — of those, the ones the enumerator offers **no affordable cast for**. The
     *    enumerator is the real authority on castability, so this is "the amount is there and something
     *    else blocks it". Colours are the dominant something in limited, but not the only one
     *    (additional costs, cast restrictions), which is why the *comparison across colour counts* is
     *    the test rather than the raw rate.
     *  - [strandedCards] — distinct cards ever stranded, so one unplayable bomb held for ten turns
     *    cannot look like ten problems.
     *
     * A strand rate on its own proves nothing about the AI: a three-colour deck is genuinely harder to
     * cast, so a *perfect* player would strand more with one too. [fixable] and [fixMissed] are the
     * pair that does attribute it. At a window where the land drop is still available, each land in
     * hand is played in a copy of the state and the casts are re-enumerated:
     *
     *  - [fixable] — **turns** in which, at some window, a land in hand would have made a stranded
     *    card castable.
     *  - [fixMissed] — of those turns, the ones in which the AI never played any of those lands.
     *
     * Counted per turn, not per window, on purpose: an AI that casts something first and plays the
     * fixing land later the same turn is not making a mistake, and a per-window count would score every
     * window before the land as a miss. [fixMissed] is an error the AI owns — it held both the answer
     * and the question for a whole turn. It is still a lower bound: a turn whose *first* window came
     * after a wrong land drop has no drop left to check.
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
        val bySeat = seatIds.withIndex().associate { (seat, id) -> id to seat }
        val decklists = seatIds.mapIndexed { seat, id ->
            id to OpponentModel.KnownDecklist(decks[seat].groupingBy { it }.eachCount())
        }.toMap()
        val players = seatIds.mapIndexed { seat, id ->
            AIPlayer.create(registry, id, seatProfiles?.get(seat) ?: profile, decklists)
        }
        fun aiFor(playerId: EntityId) = players[bySeat.getValue(playerId)]

        var state: GameState = init.state
        var actionCount = 0
        var illegal = 0
        var lastActivePlayer: EntityId? = null
        var lastProgressAction = 0
        var reason = ""
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
        val maxPlayerTurns = maxTurnsPerSeat * decks.size
        try {
            while (!state.gameOver && state.turnNumber < maxPlayerTurns && actionCount < maxActions) {
                if (actionCount - lastProgressAction > STUCK_ACTIONS_PER_TURN) {
                    reason = "stuck(turn=${state.turnNumber},step=${state.step.name})"
                    break
                }
                if (state.activePlayerId != lastActivePlayer) {
                    lastActivePlayer = state.activePlayerId
                    lastProgressAction = actionCount
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
                    val castable = enumerator.enumerate(state, probeId, EnumerationMode.ACTIONS_ONLY)
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
                        if (cardId !in castable) {
                            probeStranded++
                            probeStrandedIds += cardId
                            strandedHere += cardId
                        }
                    }
                    if (windowCounted) probeWindows++
                    // Could a land in hand have cast one of them? Only asked when something is
                    // stranded and a land drop is still available, so the common window pays nothing.
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
                val action = aiFor(priorityPlayer).chooseAction(state)
                if (probeId != null && priorityPlayer == probeId && action is PlayLand && turnLandPlayed == null) {
                    turnLandPlayed = action.cardId
                }
                val r = processor.process(state, action).result
                val next = if (r.error != null) {
                    illegal++
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
        val winnerSeat = if (state.gameOver) state.winnerId?.let { bySeat[it] } else null
        return Outcome(
            winnerSeat, state.turnNumber, actionCount, illegal, seatIds.map { state.lifeTotal(it) }, reason,
            StrandedProbe(
                probeWindows, probeAffordable, probeStranded, probeStrandedIds.size,
                probeFixable, probeFixMissed,
            ),
        )
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
    }
}
