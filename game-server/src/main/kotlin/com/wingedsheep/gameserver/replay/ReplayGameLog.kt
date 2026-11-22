package com.wingedsheep.gameserver.replay

import com.wingedsheep.ai.insight.GameLogWriter
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.engine.registry.TokenArtRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId

/** A rendered replay log and how faithfully the re-simulation behind it reproduced the game. */
data class ReplayGameLogResult(
    val text: String,
    val fidelity: ReplayFidelity,
    val actionsApplied: Int,
    val divergenceReason: String?,
)

/**
 * Re-simulates a recorded [CompactReplay] and renders it with [GameLogWriter] — the same per-game
 * log `GameLogBenchmark` writes for AI self-play — so a game the engine AI played against a real
 * player on the server can be read for misplays.
 *
 * Full information: every seat's hand is shown each turn. For the seats in `aiSeats`, a pass at a
 * decision point lists the plays that were on offer and each action lists its legal alternatives, re-derived from
 * the reconstructed position the AI acted in. Re-simulation uses the replay's pinned card
 * definitions, as the server's viewer does ([ReplayReconstructor]); a replay that no longer
 * re-simulates (core rules changed since its `engineVersion`) is cut at the divergence and says so
 * at the top, rather than inventing the rest of a game nobody played.
 */
class ReplayGameLog(
    private val cardRegistry: CardRegistry,
    private val printingRegistry: PrintingRegistry? = null,
    private val tokenArtRegistry: TokenArtRegistry? = null,
) {
    /**
     * [serverWinner] is the result the server's game summary recorded, for the games whose replay
     * carries none (one that ended by concession or disconnect).
     */
    fun render(replay: CompactReplay, aiSeats: Set<String>, serverWinner: String? = null): ReplayGameLogResult {
        val winner = replay.winnerName ?: serverWinner
        val players = replay.setup.players
        val seatIds = players.map { EntityId(it.playerId) }
        val isAi = seatIds.zip(players).associate { (id, p) -> id to (p.name in aiSeats) }
        val labels = seatIds.zip(players).associate { (id, p) ->
            id to if (isAi.getValue(id) && !p.name.startsWith("[AI]")) "[AI] ${p.name}" else p.name
        }
        val enumerator = LegalActionEnumerator.create(ReplayCardPin.overlay(cardRegistry, replay.pinnedCards))
        val body = GameLogWriter(seatIds, enumerator, labels)

        val walk = ReplayReconstructor(cardRegistry, printingRegistry, tokenArtRegistry).walk(replay) { step ->
            val state = step.before
            body.turnIfNew(state)
            val action = step.action
            val ai = isAi[action.playerId] == true
            when {
                action is SubmitDecision -> {
                    val decision = state.pendingDecision
                    if (decision != null) body.decision(state, decision, action.response)
                    else body.line("  [${state.step}] ${body.label(action.playerId)} answers: ${action.response}")
                }
                action is PassPriority -> if (ai && isDecisionPoint(state, action.playerId)) {
                    body.passWithOffers(state, action.playerId)
                }
                else -> {
                    body.action(state, action.playerId, action)
                    if (ai) body.options(body.offered(state, action.playerId))
                }
            }
        }
        val final = walk.finalState
        val diverged = walk.fidelity == ReplayFidelity.DIVERGED
        if (diverged) {
            body.line("\n=== DIVERGED at action ${walk.divergedAtAction} of ${replay.actions.size}: " +
                "${walk.divergenceReason} — the log stops here ===")
        } else if (!final.gameOver) {
            body.line("\n=== RECORD ENDS turn=${final.turnNumber} with the game still running — it ended off the " +
                "action stream (a concession, timeout or disconnect); server result: ${winner ?: "none"} ===")
        } else {
            body.line("\n=== END turn=${final.turnNumber} winner=${final.winnerId?.let(body::label) ?: "none"} " +
                "life=${seatIds.map { final.lifeTotal(it) }} ===")
        }
        body.board(final)

        val head = StringBuilder()
        head.appendLine("REPLAY ${replay.gameId}  ended=${replay.endedAt}" +
            (replay.tournamentName?.let { "  tournament=\"$it\"" + (replay.tournamentRound?.let { r -> " round $r" } ?: "") } ?: ""))
        head.appendLine("format=${replay.setup.format}  engineVersion=${replay.engineVersion}  " +
            "actions=${replay.actions.size}  winner=${winner ?: "none"}" +
            (if (winner != null && players.any { it.name == winner && it.name in aiSeats }) " (the AI)" else ""))
        head.appendLine("AI: ${seatIds.filter { isAi.getValue(it) }.joinToString { labels.getValue(it) }.ifEmpty { "none" }}" +
            "   human: ${seatIds.filterNot { isAi.getValue(it) }.joinToString { labels.getValue(it) }.ifEmpty { "none" }}")
        head.appendLine(when (walk.fidelity) {
            ReplayFidelity.EXACT -> "FIDELITY: EXACT — every action re-applied and every checkpoint matched"
            ReplayFidelity.UNVERIFIED -> "FIDELITY: UNVERIFIED — every action re-applied, but the record has no checkpoints to prove it"
            ReplayFidelity.DIVERGED -> "FIDELITY: DIVERGED at action ${walk.divergedAtAction} of ${replay.actions.size} " +
                "(current code no longer reproduces the game recorded on ${replay.engineVersion}): ${walk.divergenceReason}. " +
                "The log stops there."
        })
        if (replay.truncated) head.appendLine("NOTE: the recording was frozen before the game ended; the record is a prefix.")
        players.forEach { p ->
            val deck = p.deck.cards.map { PRINTING.replace(it, "") }.groupingBy { it }.eachCount().entries.joinToString(", ") { "${it.value}x ${it.key}" }
            head.appendLine("${labels.getValue(EntityId(p.playerId))} deck: $deck")
        }
        return ReplayGameLogResult(
            text = head.toString() + body.out.toString(),
            fidelity = walk.fidelity,
            actionsApplied = walk.divergedAtAction ?: replay.actions.size,
            divergenceReason = walk.divergenceReason,
        )
    }

    /**
     * Where a pass is worth listing what was on offer: the AI's own main phase with an empty stack
     * (it did nothing), any pass over an opponent's spell or ability (it didn't respond), and the
     * opponent's combat and end step (the instant-speed windows). Upkeep and draw-step passes with an
     * instant in hand are routine and would bury the lines that matter.
     */
    private fun isDecisionPoint(state: GameState, playerId: EntityId): Boolean {
        // Passing over its own spell is just letting it resolve; passing over the opponent's is a
        // decision not to respond.
        state.stack.lastOrNull()?.let { return controllerOf(state, it) != playerId }
        val step = state.step.name
        return if (state.activePlayerId == playerId) "MAIN" in step
        else step in OPPONENT_WINDOWS
    }

    private fun controllerOf(state: GameState, stackItem: EntityId): EntityId? {
        val e = state.getEntity(stackItem) ?: return null
        return e.get<ControllerComponent>()?.playerId
            ?: e.get<TriggeredAbilityOnStackComponent>()?.controllerId
            ?: e.get<ActivatedAbilityOnStackComponent>()?.controllerId
    }

    private companion object {
        /** A deck entry's pinned printing, `Name#SET-123` — noise in a log. */
        val PRINTING = Regex("#[^,]*$")

        val OPPONENT_WINDOWS = setOf("DECLARE_ATTACKERS", "DECLARE_BLOCKERS", "END")
    }
}
