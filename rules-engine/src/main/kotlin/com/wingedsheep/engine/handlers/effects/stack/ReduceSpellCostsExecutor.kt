package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.SpellCostReduction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ReduceSpellCostsEffect
import kotlin.reflect.KClass

/**
 * Executor for [ReduceSpellCostsEffect].
 *
 * Evaluates the discount **once, here**, and records a [SpellCostReduction] on the game state.
 * The cost calculator applies it to every matching spell the controller casts until the effect's
 * duration ends — [com.wingedsheep.engine.core.TurnManager.startTurn] clears end-of-turn entries,
 * [com.wingedsheep.engine.core.CleanupPhaseManager.expireUntilYourNextTurnEffects] the
 * "until your next turn" ones.
 *
 * Resolving the amount now rather than per cast is what the Scion cycle's rulings require — "the
 * value of X is determined only once, at the time the ability resolves". Mirrors
 * [GrantNextSpellAffinityExecutor], which installs the one-shot variant of the same idea.
 *
 * A resolved amount of 0 or less installs nothing: the discount would be a no-op, and skipping it
 * keeps the state (and the client's cost display) free of dead entries.
 */
class ReduceSpellCostsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<ReduceSpellCostsEffect> {

    override val effectType: KClass<ReduceSpellCostsEffect> = ReduceSpellCostsEffect::class

    override fun execute(
        state: GameState,
        effect: ReduceSpellCostsEffect,
        context: EffectContext
    ): EffectResult {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) return EffectResult.success(state)

        val (effectiveState, sourceId) = if (context.sourceId != null) {
            state to context.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val sourceName = effectiveState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val reduction = SpellCostReduction(
            controllerId = context.controllerId,
            spellFilter = effect.spellFilter,
            amount = amount,
            sourceId = sourceId,
            sourceName = sourceName,
            duration = effect.duration,
        )
        return EffectResult.success(
            effectiveState.copy(
                spellCostReductions = effectiveState.spellCostReductions + reduction
            )
        )
    }
}
