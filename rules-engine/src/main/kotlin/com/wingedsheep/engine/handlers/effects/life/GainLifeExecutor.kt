package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import kotlin.reflect.KClass

/**
 * Executor for GainLifeEffect.
 * "You gain X life" or "Target player gains X life"
 */
class GainLifeExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<GainLifeEffect> {

    override val effectType: KClass<GainLifeEffect> = GainLifeEffect::class

    override fun execute(
        state: GameState,
        effect: GainLifeEffect,
        context: EffectContext
    ): EffectResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
        if (playerIds.isEmpty()) {
            return EffectResult.error(state, "No valid target for life gain")
        }

        val amount = amountEvaluator.evaluate(state, effect.amount, context)

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (playerId in playerIds) {
            val (gainedState, event) = DamageUtils.gainLife(newState, playerId, amount, predicateEvaluator = amountEvaluator.predicates)
            newState = gainedState
            if (event != null) events.add(event)
        }

        return EffectResult.success(newState, events)
    }
}
