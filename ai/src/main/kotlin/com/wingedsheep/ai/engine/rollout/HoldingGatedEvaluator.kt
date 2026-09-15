package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.evaluation.ManaReserve
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Rollouts only where keeping mana up is the question — mtg-draft-ai `docs/28` §7.
 *
 * A one-ply leaf cannot price untapped mana on our own turn: what the mana buys happens on the
 * opponent's turn, which no simulation from the candidate reaches, so [ManaReserve] as a static bonus
 * only cost tempo (`docs/27` §7.4.1). A playout does reach it — our policy may cast the held answer
 * there — but rollouts on every decision cost ~30× a game. So this scores a decision with [rollout] only
 * when [ManaReserve.holdsUpAnswer] holds at the root with an empty stack (our turn, an instant-speed answer
 * in hand that our untapped lands pay for, something across the table for it to answer), and with
 * [static] everywhere else.
 */
class HoldingGatedEvaluator(
    private val intents: IntentCatalog,
    private val rollout: CandidateEvaluator,
    private val static: CandidateEvaluator,
) : CandidateEvaluator {

    override fun score(root: GameState, afterAction: GameState, playerId: EntityId, budget: DecisionBudget): Double =
        pick(root, playerId).score(root, afterAction, playerId, budget)

    override fun scoreAll(
        root: GameState,
        afterActions: List<GameState>,
        playerId: EntityId,
        budget: DecisionBudget,
    ): List<Double> = pick(root, playerId).scoreAll(root, afterActions, playerId, budget)

    /** Whether a decision from [root] is one this evaluator hands to the rollouts. */
    fun holding(root: GameState, playerId: EntityId): Boolean =
        root.stack.isEmpty() && ManaReserve.holdsUpAnswer(root, root.projectedState, playerId, intents)

    private fun pick(root: GameState, playerId: EntityId): CandidateEvaluator =
        if (holding(root, playerId)) rollout else static

    override fun toString(): String = "rollout-when-holding($rollout)"
}
