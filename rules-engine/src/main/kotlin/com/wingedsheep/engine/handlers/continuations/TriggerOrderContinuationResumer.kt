package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.TriggerOrderContinuation
import com.wingedsheep.engine.core.TriggerOrderResponse
import com.wingedsheep.engine.mechanics.TriggerOrdering
import com.wingedsheep.engine.state.GameState

/**
 * Resumes [TriggerOrderContinuation]: rearranges the answering player's part of the waiting wave,
 * then asks the next player in APNAP order who has triggers to order (CR 603.3b). Once nobody is
 * left, the settle that follows the answer puts the wave on the stack.
 */
class TriggerOrderContinuationResumer : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(TriggerOrderContinuation::class, ::resumeTriggerOrder),
    )

    private fun resumeTriggerOrder(
        state: GameState,
        continuation: TriggerOrderContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val count = state.triggersBeingPlaced.count { it.controllerId == continuation.playerId }
        if (response !is TriggerOrderResponse || response.resolutionOrder.sorted() != (0 until count).toList()) {
            return ExecutionResult.error(state, "Order each triggered ability exactly once")
        }
        val reordered = TriggerOrdering.reorder(state, continuation.playerId, response.resolutionOrder)
        return TriggerOrdering.ask(reordered, after = continuation.playerId) ?: checkForMore(reordered, emptyList())
    }
}
