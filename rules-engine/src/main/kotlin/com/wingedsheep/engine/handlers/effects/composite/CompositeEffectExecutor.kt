package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlin.reflect.KClass

/**
 * Executor for CompositeEffect: starts an [com.wingedsheep.engine.core.EffectProgram] whose root
 * is the composite and runs it with [EffectProgramInterpreter]. Nested composites are positions in
 * that one program; if a step pauses for a decision, the program's frame waits beneath it.
 *
 * @param effectExecutor The registry's plain entry point. Each child runs one resolution depth
 *   below the composite, as it did when the composite executor called the registry's recursion for
 *   each child; the program's context carries that depth, so a resumed run keeps it.
 */
class CompositeEffectExecutor(
    effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<CompositeEffect> {

    override val effectType: KClass<CompositeEffect> = CompositeEffect::class

    private val interpreter = EffectProgramInterpreter(effectExecutor)

    override fun execute(
        state: GameState,
        effect: CompositeEffect,
        context: EffectContext
    ): EffectResult = interpreter.run(state, effect, context.copy(resolutionDepth = context.resolutionDepth + 1))
}
