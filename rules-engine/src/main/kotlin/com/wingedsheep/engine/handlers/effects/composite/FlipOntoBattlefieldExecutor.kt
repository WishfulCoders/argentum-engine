package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.FlipOntoBattlefieldContinuation
import com.wingedsheep.engine.core.FlipOntoBattlefieldDecision
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.dexterity.FlipTableLayout
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.FlipOntoBattlefieldEffect
import kotlin.reflect.KClass

/**
 * Executor for [FlipOntoBattlefieldEffect]: lay the battlefield out as a table and ask the
 * controller to throw. The landing is rolled when they answer
 * ([com.wingedsheep.engine.handlers.continuations.MiscContinuationResumer]), so the throw — not
 * the dice — is the player's.
 */
class FlipOntoBattlefieldExecutor(
    private val predicateEvaluator: PredicateEvaluator,
) : EffectExecutor<FlipOntoBattlefieldEffect> {

    override val effectType: KClass<FlipOntoBattlefieldEffect> = FlipOntoBattlefieldEffect::class

    override fun execute(state: GameState, effect: FlipOntoBattlefieldEffect, context: EffectContext): EffectResult {
        val flipper = context.controllerId
        val sourceCard = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>() }
        val sourceName = sourceCard?.name ?: "Card"
        val predicateContext = PredicateContext.fromEffectContext(context)
        val projected = state.projectedState
        val table = FlipTableLayout.build(state, flipper, exclude = context.sourceId) { id ->
            predicateEvaluator.matches(state, projected, id, effect.filter, predicateContext)
        }

        val continuation = FlipOntoBattlefieldContinuation(
            playerId = flipper,
            sourceId = context.sourceId,
            sourceName = sourceName,
            sourceImageUri = sourceCard?.imageUri,
            table = table,
            storeTouchedAs = effect.storeTouchedAs,
        )
        return EffectResult.from(
            state.suspendForDecision(
                { id ->
                    FlipOntoBattlefieldDecision(
                        id = id,
                        playerId = flipper,
                        prompt = "Flip $sourceName onto the battlefield",
                        context = DecisionContext(
                            sourceId = context.sourceId,
                            sourceName = sourceName,
                            phase = DecisionPhase.RESOLUTION,
                        ),
                        table = table,
                        flippedImageUri = sourceCard?.imageUri,
                    )
                },
                continuation,
            )
        )
    }
}
