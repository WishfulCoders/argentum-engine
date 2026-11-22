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
    private val amountEvaluator: DynamicAmountEvaluator,
    private val replacementProcessor: com.wingedsheep.engine.replacement.ReplacementEffectProcessor,
    private val executeEffect: (GameState, com.wingedsheep.sdk.scripting.effects.Effect, EffectContext) -> EffectResult
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

        // One gain per player, in order. A replacement can pause one of them; the gains after it
        // are queued beneath as a program over all of them, its cursor at the gain in progress.
        // The root is synthetic — this gain expanded per player, the gains before the cursor having
        // run in this call rather than in the program — so its origin is a `BuiltBy` label and the
        // cursor a local position in that expansion, not a path into any authored tree.
        val perPlayer by lazy {
            com.wingedsheep.sdk.scripting.effects.CompositeEffect(playerIds.map {
                com.wingedsheep.sdk.dsl.Effects.GainLife(amount,
                    com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(it))
            })
        }
        // The label the registry handed down to this call names this gain. The program's leaves
        // are this gain's remaining per-player parts, so they run under the label this call ran
        // under (its parent), and the registry labels each exactly as it labelled this call.
        val origin = context.programOrigin ?: com.wingedsheep.engine.core.ProgramOrigin.UNATTRIBUTED
        val leafContext = context.copy(programOrigin = (origin as? com.wingedsheep.engine.core.ProgramOrigin.BuiltBy)?.parent)
        for ((index, playerId) in playerIds.withIndex()) {
            val isLast = index == playerIds.lastIndex
            val prepared = if (isLast) newState else newState.pushContinuation(
                com.wingedsheep.engine.core.EffectProgram(
                    root = perPlayer,
                    origin = origin,
                    cursor = com.wingedsheep.engine.core.ProgramCursor(listOf(
                        com.wingedsheep.engine.handlers.effects.composite.EffectProgramInterpreter.rootStep(index, leafContext))),
                    effectContext = leafContext,
                ))
            val result = if (LifeGainReplacements.applies(prepared, playerId, amount, amountEvaluator.predicates)) {
                LifeGainReplacements.resolve(prepared, playerId, amount, context, replacementProcessor, executeEffect)
            } else {
                val (gained, event) = DamageUtils.gainLife(prepared, playerId, amount, predicateEvaluator = amountEvaluator.predicates)
                EffectResult.success(gained, listOfNotNull(event))
            }
            events.addAll(result.events)
            if (result.outcome is com.wingedsheep.engine.core.Outcome.Paused)
                return result.copy(events = events)
            newState = if (isLast) result.state else result.state.popContinuation().second
        }

        return EffectResult.success(newState, events)
    }
}
