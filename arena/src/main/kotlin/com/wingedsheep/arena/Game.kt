package com.wingedsheep.arena

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.hidden.OpponentModel
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.CastSpell
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
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Step
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
    /** Print the stack trace of an exception that ends a game (the `one` mode). */
    private val printTraces: Boolean = false,
    /** Count [Outcome.holding] (mtg-draft-ai `docs/28` §8); off, a game is not enumerated twice. */
    private val measureHolding: Boolean = false,
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
    )

    /** [seatProfiles] overrides [profile] seat by seat (a one-sided A/B); null plays [profile] on both. */
    fun play(decks: List<List<String>>, seed: Long, seatProfiles: List<AiProfile>? = null): Outcome {
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
        return playFrom(init.state, seatIds.indices.map { seatProfiles?.get(it) ?: profile }, decklists)
    }

    /**
     * Plays [start] to the end with [seatProfiles] seat by seat, in `turnOrder`; [decklists] are the decks each
     * AI's opponent model may assume. What [play] does after dealing, and what the replay module plays on from a
     * rebuilt game's break with (mtg-draft-ai `docs/27` §5, C1). Seats in the [Outcome] are `turnOrder` indices.
     */
    fun playFrom(start: GameState, seatProfiles: List<AiProfile>, decklists: Map<EntityId, OpponentModel>): Outcome {
        val seatIds = start.turnOrder
        val bySeat = seatIds.withIndex().associate { (seat, id) -> id to seat }
        val players = seatIds.mapIndexed { seat, id -> AIPlayer.create(registry, id, seatProfiles[seat], decklists) }
        fun aiFor(playerId: EntityId) = players[bySeat.getValue(playerId)]

        var state: GameState = start
        var actionCount = 0
        var illegal = 0
        var lastActivePlayer: EntityId? = null
        var lastProgressAction = 0
        var reason = ""
        val holding = if (measureHolding) seatIds.map { IntArray(3) } else null
        val cycle = if (measureHolding) seatIds.map { IntArray(6) } else null
        val casts = if (measureHolding) seatIds.map { mutableMapOf<String, Int>() } else null
        val tappedOut = if (measureHolding) seatIds.map { IntArray(2) } else null
        // Each seat's lands and untapped lands at the last step seen, committed when the turn passes.
        val lastLands = IntArray(seatIds.size)
        val lastUntapped = IntArray(seatIds.size)
        var cycleActive: EntityId? = null
        val maxPlayerTurns = maxTurnsPerSeat * seatIds.size
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
                actionCount++
                val instants = if (holding != null) affordableInstants(state, priorityPlayer) else emptySet()
                val action = aiFor(priorityPlayer).chooseAction(state)
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
        val winnerSeat = if (state.gameOver) state.winnerId?.let { bySeat[it] } else null
        return Outcome(
            winnerSeat, state.turnNumber, actionCount, illegal, seatIds.map { state.lifeTotal(it) }, reason,
            holding?.map { it.toList() }, cycle?.map { it.toList() }, casts, tappedOut?.map { it.toList() },
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
    private fun affordableInstants(state: GameState, playerId: EntityId): Set<EntityId> {
        if (state.activePlayerId != playerId || !state.step.isMainPhase || state.stack.isNotEmpty()) return emptySet()
        return enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY).mapNotNull { la ->
            (la.action as? CastSpell)?.cardId?.takeIf {
                la.affordable && state.getEntity(it)?.get<CardComponent>()?.typeLine?.isInstant == true
            }
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
    }
}
