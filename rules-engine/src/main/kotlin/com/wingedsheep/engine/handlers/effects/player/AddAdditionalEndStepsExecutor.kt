package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.AdditionalEndStepsComponent
import com.wingedsheep.sdk.scripting.effects.AddAdditionalEndStepsEffect
import kotlin.reflect.KClass

/**
 * Executor for [AddAdditionalEndStepsEffect] (Y'shtola Rhul).
 *
 * "There is an additional end step after this step." Per CR 500.9 the step is inserted directly
 * after the current end step; per CR 500.10a these "there is an additional end step" riders are
 * always added to the controller's own turn (they only ever trigger on the controller's end step,
 * so the active player when this resolves is that controller). Accumulates the resolved amount onto
 * an [AdditionalEndStepsComponent] on the active player; the TurnManager drains it when advancing
 * out of the end step, redirecting back into a fresh end step (CR 513) instead of the cleanup step.
 */
class AddAdditionalEndStepsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<AddAdditionalEndStepsEffect> {

    override val effectType: KClass<AddAdditionalEndStepsEffect> =
        AddAdditionalEndStepsEffect::class

    override fun execute(
        state: GameState,
        effect: AddAdditionalEndStepsEffect,
        context: EffectContext
    ): EffectResult {
        val activePlayer = state.activePlayerId
            ?: return EffectResult.error(state, "No active player for AddAdditionalEndStepsEffect")

        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return EffectResult.success(state)
        }

        val existing = state.getEntity(activePlayer)?.get<AdditionalEndStepsComponent>()
        val newCount = (existing?.count ?: 0) + amount

        val newState = state.updateEntity(activePlayer) { container ->
            container.with(AdditionalEndStepsComponent(count = newCount))
        }

        return EffectResult.success(newState)
    }
}
