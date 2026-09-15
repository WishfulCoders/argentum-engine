package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.budget.BudgetTier
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.budget.SearchAllowances
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * [HoldingGatedEvaluator] hands a decision to the rollouts only when keeping mana up is the question,
 * and otherwise to the static leaf, for single scores and batches alike.
 */
class HoldingGatedEvaluatorTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    /** Answers every state with one constant, so which evaluator ran shows in the score. */
    private class Constant(private val value: Double) : CandidateEvaluator {
        override fun score(root: GameState, afterAction: GameState, playerId: EntityId, budget: DecisionBudget) = value
    }

    private val budget = DecisionBudget(BudgetTier.NORMAL, SearchAllowances.LEGACY, DecisionBudget.UNBOUNDED_MILLIS)

    private fun gated() = HoldingGatedEvaluator(intents, rollout = Constant(ROLLOUT), static = Constant(STATIC))

    private fun root(answer: String = "Counterspell", islands: Int = 2, active: Int = 1) = scenario()
        .withPlayers()
        .withActivePlayer(active)
        .withLandsOnBattlefield(1, "Island", islands)
        .withCardInHand(1, answer)
        .withCardInHand(2, "Craw Wurm")
        .build()

    init {
        test("our turn, a counterspell in hand and the lands to pay for it: the rollouts") {
            val game = root()
            gated().score(game.state, game.state, game.player1Id, budget) shouldBe ROLLOUT
            gated().scoreAll(game.state, listOf(game.state, game.state), game.player1Id, budget) shouldContainExactly
                listOf(ROLLOUT, ROLLOUT)
        }

        test("one land short, a sorcery-speed card, or the opponent's turn: the static leaf") {
            listOf(root(islands = 1), root(answer = "Wrath of God", islands = 4), root(active = 2)).forEach { game ->
                gated().score(game.state, game.state, game.player1Id, budget) shouldBe STATIC
                gated().scoreAll(game.state, listOf(game.state), game.player1Id, budget) shouldContainExactly listOf(STATIC)
            }
        }
    }

    private companion object {
        const val ROLLOUT = 1.0
        const val STATIC = -1.0
    }
}
