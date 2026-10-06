package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.sdk.scripting.effects.AllowAdditionalLoyaltyActivationsThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [AllowAdditionalLoyaltyActivationsThisTurnEffect] — adds to the target permanent's
 * loyalty allowance for the turn (CR 606.3's one activation per permanent per turn, plus N).
 *
 * The bonus is stored on the permanent's turn-scoped [AbilityActivatedThisTurnComponent] next to
 * the "rather than only once" limit, so it lapses at cleanup and when the permanent changes zones
 * (CR 400.7). It is a sum: each resolution adds [AllowAdditionalLoyaltyActivationsThisTurnEffect.count].
 *
 * A permission, not a state change anything reacts to, so no event is emitted (like
 * [AllowLoyaltyActivationsThisTurnExecutor]).
 */
class AllowAdditionalLoyaltyActivationsThisTurnExecutor :
    EffectExecutor<AllowAdditionalLoyaltyActivationsThisTurnEffect> {

    override val effectType: KClass<AllowAdditionalLoyaltyActivationsThisTurnEffect> =
        AllowAdditionalLoyaltyActivationsThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: AllowAdditionalLoyaltyActivationsThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)
        // A planeswalker that left the battlefield before this resolved is a new object;
        // nothing to grant.
        if (targetId !in state.getBattlefield()) return EffectResult.success(state)

        val newState = state.updateEntity(targetId) { container ->
            val tracker = container.get<AbilityActivatedThisTurnComponent>() ?: AbilityActivatedThisTurnComponent()
            container.with(tracker.withLoyaltyActivationBonus(effect.count))
        }
        return EffectResult.success(newState)
    }
}
