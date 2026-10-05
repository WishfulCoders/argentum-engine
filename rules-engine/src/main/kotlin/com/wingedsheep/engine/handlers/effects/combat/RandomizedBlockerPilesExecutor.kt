package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.BlockerDeclarationPolicyChangedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.RandomizedBlockerPilesEffect
import kotlin.reflect.KClass

class RandomizedBlockerPilesExecutor : EffectExecutor<RandomizedBlockerPilesEffect> {
    override val effectType: KClass<RandomizedBlockerPilesEffect> = RandomizedBlockerPilesEffect::class

    override fun execute(state: GameState, effect: RandomizedBlockerPilesEffect, context: EffectContext): EffectResult =
        EffectResult.success(
            state.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.RandomizedBlockerPiles,
                affectedEntities = emptySet(),
                duration = effect.duration,
                context = context,
            ),
            listOf(BlockerDeclarationPolicyChangedEvent),
        )
}
