package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.StoreCardNameEffect
import kotlin.reflect.KClass

/**
 * Executor for [StoreCardNameEffect].
 *
 * Reads the name of the first card in the [StoreCardNameEffect.from] collection and emits
 * it via [EffectResult.updatedChosenValues]. The surrounding
 * [com.wingedsheep.engine.handlers.effects.composite.EffectProgramInterpreter] merges the
 * value into the pipeline context so later sub-effects can match it via
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.NameEqualsChosen].
 *
 * No-op (success, nothing stored) when the collection is empty or missing — e.g. the
 * player had nothing to choose, mirroring "do as much as possible" resolution.
 */
class StoreCardNameExecutor : EffectExecutor<StoreCardNameEffect> {

    override val effectType: KClass<StoreCardNameEffect> = StoreCardNameEffect::class

    override fun execute(
        state: GameState,
        effect: StoreCardNameEffect,
        context: EffectContext
    ): EffectResult {
        val cardId = context.pipeline.storedCollections[effect.from]?.firstOrNull()
            ?: return EffectResult.success(state)
        val name = state.getEntity(cardId)?.get<CardComponent>()?.name
            ?: return EffectResult.success(state)
        return EffectResult(state = state, updatedChosenValues = mapOf(effect.storeAs to name))
    }
}
