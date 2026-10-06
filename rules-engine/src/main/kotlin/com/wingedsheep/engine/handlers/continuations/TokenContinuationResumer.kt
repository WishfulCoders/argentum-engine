package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.EntersAttackingDefenderContinuation
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.TokenCreationReplacementContinuation
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.effects.token.TokenCreationReplacementHelper
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.core.Outcome

/**
 * Handles token-related continuation resumptions:
 * - TokenCreationReplacementContinuation (Mirrormind Crown yes/no)
 * - EntersAttackingDefenderContinuation (what a "tapped and attacking that player or a planeswalker
 *   they control" token attacks — Adeline, Resplendent Cathar)
 */
class TokenContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(TokenCreationReplacementContinuation::class, ::resumeTokenCreationReplacement),
        resumer(EntersAttackingDefenderContinuation::class, ::resumeEntersAttackingDefender)
    )

    /**
     * Record what one entering token attacks and re-run the token effect: it asks for the next
     * token's pick, or creates every token once all have one (CR 508.4). The created tokens are
     * handed to a consuming pipeline frame beneath, as the uninterrupted effect would have.
     */
    private fun resumeEntersAttackingDefender(
        state: GameState,
        continuation: EntersAttackingDefenderContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected a target response for what the token attacks")
        }
        val picked = response.selectedTargets[0]?.singleOrNull()
            ?.takeIf { it in continuation.options }
            ?: return ExecutionResult.error(state, "Choose exactly one of the offered players or planeswalkers")

        val context = continuation.effectContext
        val chosen = continuation.chosen + picked
        val rerun = context.copy(
            pipeline = context.pipeline.copy(
                storedCollections = context.pipeline.storedCollections +
                    (com.wingedsheep.engine.handlers.effects.token.EntersAttackingDefenders.CHOICES_KEY to chosen)
            )
        )
        val effectResult = services.effectExecutorRegistry.execute(state, continuation.originalEffect, rerun)
        if (effectResult.outcome is Outcome.Paused) return effectResult.toExecutionResult()
        val exposed = exposeCollectionsToNextFrame(effectResult.state, effectResult.updatedCollections)
        return checkForMore(exposed, effectResult.events)
    }

    private fun resumeTokenCreationReplacement(
        state: GameState,
        continuation: TokenCreationReplacementContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for token creation replacement")
        }

        val context = continuation.effectContext

        if (response.choice) {
            // Player chose to replace: create copies of the attached permanent.
            // Pass cardRegistry so the token applies the attached permanent's printed
            // "enters with N counters" replacement effects (per Mirrormind Crown rulings).
            val result = TokenCreationReplacementHelper.createAttachedPermanentCopies(
                state,
                continuation.attachedPermanentId,
                context.controllerId,
                continuation.tokenCount,
                cardRegistry = services.cardRegistry,
                staticAbilityHandler = StaticAbilityHandler(services.cardRegistry),
                predicateEvaluator = services.predicateEvaluator
            )
            if (result.outcome is Outcome.Paused) return result.toExecutionResult()
            return checkForMore(result.state, result.events)
        } else {
            // Player declined: execute original token creation effect
            // The source is already marked as "offered this turn" so the replacement
            // won't fire again when we re-execute the original effect.
            val effectResult = services.effectExecutorRegistry.execute(
                state,
                continuation.originalEffect,
                context
            )
            if (effectResult.outcome is Outcome.Paused) return effectResult.toExecutionResult()
            return checkForMore(effectResult.state, effectResult.events)
        }
    }
}
