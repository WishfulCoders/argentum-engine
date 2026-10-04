package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import java.util.Locale

/**
 * How the engine and the AI scale with the number of creature tokens on the battlefield — the
 * measurement behind [com.wingedsheep.engine.core.GameLimits.MAX_TOKENS_ON_BATTLEFIELD]. A loop
 * shortcut can put hundreds of tokens into play in one decision, and every token is an entity that
 * each projection, legal-action pass and combat decision then scans.
 *
 * For each board size N the active player has N untapped 2/2 tokens and the opponent N/2. Measured
 * on that board: legal-action enumeration, an uncached projection, and one whole turn (main phase
 * through the opponent's next untap) played by the default [AIPlayer] on both seats — which is where
 * attack and block choice over N creatures shows up. A turn that runs past [turnBudgetMs] is cut off
 * and reported as such; larger boards are then skipped.
 *
 * Disabled by default. Run with:
 *   ./gradlew :ai:test --tests "*.TokenScalingBenchmark" -Dbenchmark=true
 *   (BENCHMARK_TOKENS=0,25,50,100,200,400 in the environment chooses the sizes)
 */
class TokenScalingBenchmark : ScenarioTestBase() {

    private val benchmarkEnabled = System.getProperty("benchmark") == "true"
    private val sizes = (System.getenv("BENCHMARK_TOKENS") ?: "0,25,50,100,200,400,800")
        .split(',').map { it.trim().toInt() }
    private val turnBudgetMs = System.getenv("BENCHMARK_TURN_BUDGET_MS")?.toLongOrNull() ?: 120_000L

    private fun board(n: Int): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withLifeTotal(1, 1_000_000)
            .withLifeTotal(2, 1_000_000)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) {
            builder = builder.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
        }
        repeat(n) { builder = builder.withCardOnBattlefield(1, "Grizzly Bears", isToken = true) }
        repeat(n / 2) { builder = builder.withCardOnBattlefield(2, "Grizzly Bears", isToken = true) }
        return builder.build()
    }

    private inline fun timeMs(block: () -> Unit): Double {
        val t0 = System.nanoTime()
        block()
        return (System.nanoTime() - t0) / 1e6
    }

    init {
        test("benchmark: engine and AI cost vs tokens on the battlefield").config(enabled = benchmarkEnabled) {
            val enumerator = LegalActionEnumerator.create(cardRegistry)
            println("tokens(active/opp)  enumerate_ms  project_ms  turn_s  turn_actions  ms/action  note")
            for (n in sizes) {
                val game = board(n)
                val p1 = game.player1Id
                val p2 = game.player2Id

                // Warm once, then average a few runs of each primitive.
                enumerator.enumerate(game.state, p1)
                val enumerateMs = (1..5).map { timeMs { enumerator.enumerate(game.state, p1) } }.average()
                val projectMs = (1..5).map { timeMs { game.state.copy().projectedState } }.average()

                val ais = mapOf(p1 to AIPlayer.create(cardRegistry, p1), p2 to AIPlayer.create(cardRegistry, p2))
                val startTurn = game.state.turnNumber
                var actions = 0
                var note = ""
                val t0 = System.nanoTime()
                while (!game.state.gameOver && game.state.turnNumber == startTurn) {
                    if ((System.nanoTime() - t0) / 1_000_000 > turnBudgetMs) {
                        note = "CUT OFF at ${turnBudgetMs / 1000}s in ${game.state.step}"
                        break
                    }
                    val state: GameState = game.state
                    val decision = state.pendingDecision
                    val action: GameAction = if (decision != null) {
                        SubmitDecision(decision.playerId, ais.getValue(decision.playerId).respondToDecision(state, decision))
                    } else {
                        val actor = state.priorityPlayerId ?: break
                        ais.getValue(actor).chooseAction(state)
                    }
                    val result = game.execute(action)
                    if (result.error != null) {
                        note = "rejected ${action::class.simpleName}: ${result.error}"
                        break
                    }
                    actions++
                }
                val turnS = (System.nanoTime() - t0) / 1e9
                println(
                    String.format(
                        Locale.ROOT, "%5d/%-5d  %12.2f  %10.2f  %6.2f  %12d  %9.1f  %s",
                        n, n / 2, enumerateMs, projectMs, turnS, actions,
                        if (actions > 0) turnS * 1000 / actions else 0.0, note,
                    )
                )
                if (note.startsWith("CUT OFF")) break
            }
        }
    }
}
