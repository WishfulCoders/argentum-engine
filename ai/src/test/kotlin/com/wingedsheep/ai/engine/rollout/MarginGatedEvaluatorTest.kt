package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.budget.BudgetTier
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.budget.SearchAllowances
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

/**
 * [MarginGatedEvaluator] hands a decision to the rollouts only when the static best two are within the margin, rolls
 * out everything when there is no margin, and logs one line per scored decision.
 */
class MarginGatedEvaluatorTest : ScenarioTestBase() {

    /** Answers a batch from a fixed list, so which evaluator ran shows in the scores. */
    private class Fixed(private val values: List<Double>) : CandidateEvaluator {
        override fun score(root: GameState, afterAction: GameState, playerId: EntityId, budget: DecisionBudget) =
            values.first()

        override fun scoreAll(root: GameState, afterActions: List<GameState>, playerId: EntityId, budget: DecisionBudget) =
            values.take(afterActions.size)
    }

    private val budget = DecisionBudget(BudgetTier.NORMAL, SearchAllowances.LEGACY, DecisionBudget.UNBOUNDED_MILLIS)

    // The static leaf prefers candidate 0 by 0.5; the rollout overturns it.
    private val static = Fixed(listOf(1.0, 0.5, -2.0))
    private val rollout = Fixed(listOf(0.2, 0.9, -1.0))

    private fun gated(margin: Double?, log: String? = null) =
        MarginGatedEvaluator(rollout, static, RolloutGate(margin, log))

    init {
        test("the best two within the margin: the rollouts") {
            val game = scenario().withPlayers().build()
            val states = listOf(game.state, game.state, game.state)
            gated(margin = 1.0).scoreAll(game.state, states, game.player1Id, budget) shouldContainExactly
                listOf(0.2, 0.9, -1.0)
        }

        test("a clear static best: the static leaf") {
            val game = scenario().withPlayers().build()
            val states = listOf(game.state, game.state, game.state)
            gated(margin = 0.3).scoreAll(game.state, states, game.player1Id, budget) shouldContainExactly
                listOf(1.0, 0.5, -2.0)
        }

        test("one candidate has no runner-up, so a margin never rolls it out") {
            val game = scenario().withPlayers().build()
            gated(margin = 100.0).score(game.state, game.state, game.player1Id, budget) shouldBeExactly 1.0
        }

        test("no margin rolls out every decision, one candidate included") {
            val game = scenario().withPlayers().build()
            gated(margin = null).score(game.state, game.state, game.player1Id, budget) shouldBeExactly 0.2
        }

        test("the log records the gap and the overturn") {
            val game = scenario().withPlayers().build()
            val log = File.createTempFile("gate", ".jsonl").apply { deleteOnExit() }
            val states = listOf(game.state, game.state, game.state)
            gated(margin = null, log = log.path).scoreAll(game.state, states, game.player1Id, budget)
            MarginGatedEvaluator.flush()
            val line = log.readLines().single()
            line shouldContain "\"gap\":0.5000"
            line shouldContain "\"static_pick\":0,\"pick\":1"
            line shouldContain "\"rolled\":true"
        }

        test("topTwoGap: infinity without a runner-up or across a terminal sentinel") {
            MarginGatedEvaluator.topTwoGap(listOf(3.0)) shouldBe Double.POSITIVE_INFINITY
            MarginGatedEvaluator.topTwoGap(listOf(Double.POSITIVE_INFINITY, 1.0)) shouldBe Double.POSITIVE_INFINITY
            MarginGatedEvaluator.topTwoGap(listOf(1.0, 4.0, 2.5)) shouldBeExactly 1.5
        }
    }
}
