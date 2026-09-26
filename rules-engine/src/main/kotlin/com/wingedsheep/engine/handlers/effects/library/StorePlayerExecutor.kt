package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.StorePlayerEffect
import kotlin.reflect.KClass

/**
 * Executor for [StorePlayerEffect].
 *
 * Resolves the player reference and appends it to the named pipeline collection, emitting the
 * grown collection via [EffectResult.updatedCollections]. Appending (rather than replacing) lets
 * several records in one scope accumulate; across a `ForEachPlayerCollecting` iteration the
 * per-iteration scope starts empty and the loop's `collectCollections` does the union. A player
 * reference that resolves to nobody records nothing.
 */
class StorePlayerExecutor : EffectExecutor<StorePlayerEffect> {

    override val effectType: KClass<StorePlayerEffect> = StorePlayerEffect::class

    override fun execute(
        state: GameState,
        effect: StorePlayerEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = TargetResolutionUtils.resolvePlayerRef(effect.player, context, state)
            ?: return EffectResult.success(state)
        val existing = context.pipeline.storedCollections[effect.storeAs].orEmpty()
        val grown = if (playerId in existing) existing else existing + playerId
        return EffectResult(state = state, updatedCollections = mapOf(effect.storeAs to grown))
    }
}
