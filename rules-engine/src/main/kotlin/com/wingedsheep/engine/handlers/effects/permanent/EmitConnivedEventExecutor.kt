package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PermanentConnivedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.scripting.effects.EmitConnivedEventEffect
import kotlin.reflect.KClass

/**
 * Emits a [PermanentConnivedEvent] so "whenever a creature you control connives" triggers
 * (CR 701.50) fire. Appended by [ConniveEffectExecutor] to the tail of the connive pipeline — after
 * the discard decision resolves — so the event lands in a completed resolution batch; see
 * [com.wingedsheep.sdk.scripting.effects.EmitConnivedEventEffect].
 */
class EmitConnivedEventExecutor : EffectExecutor<EmitConnivedEventEffect> {

    override val effectType: KClass<EmitConnivedEventEffect> = EmitConnivedEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitConnivedEventEffect,
        context: EffectContext
    ): EffectResult {
        val connivingId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        val source = context.sourceId?.let { state.getEntity(it) }
        // A convoker that left before it connived (CR 701.50b) is matched by its last-known
        // information, which the resolving spell froze as it left.
        val lastKnown = source?.get<SpellOnStackComponent>()?.convokedLastKnown?.get(connivingId)
        return EffectResult.success(
            state,
            listOf(
                PermanentConnivedEvent(
                    connivingPermanentId = connivingId,
                    controllerId = context.controllerId,
                    sourceName = source?.get<CardComponent>()?.name,
                    lastKnown = lastKnown
                )
            )
        )
    }
}
