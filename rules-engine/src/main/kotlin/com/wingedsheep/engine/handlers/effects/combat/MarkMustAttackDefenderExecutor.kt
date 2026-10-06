package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.MustAttackDefenderComponent
import com.wingedsheep.engine.state.components.combat.MustAttackDefenderRequirement
import com.wingedsheep.sdk.scripting.effects.AttackRequirementWindow
import com.wingedsheep.sdk.scripting.effects.MarkMustAttackDefenderEffect
import kotlin.reflect.KClass

/**
 * Executor for [MarkMustAttackDefenderEffect] — "[creature] attacks [defender] [window] if able".
 *
 * Records a [MustAttackDefenderRequirement] on the creature, naming the defender by entity and,
 * for a permanent, by object generation, so a defender that leaves and returns is a new object the
 * requirement no longer names (CR 400.7). A [AttackRequirementWindow.THIS_TURN] requirement is in
 * force at once; a [AttackRequirementWindow.CONTROLLERS_NEXT_TURN] one waits, unarmed, until the
 * next turn taken by whoever controls the creature then (armed by `TurnManager`). Enforced at
 * declare attackers under CR 508.1d by `AttackPhaseManager`.
 *
 * Does nothing when the creature is no longer on the battlefield (an "up to one" target left
 * unchosen, or a target that became illegal is handled upstream).
 */
class MarkMustAttackDefenderExecutor : EffectExecutor<MarkMustAttackDefenderEffect> {

    override val effectType: KClass<MarkMustAttackDefenderEffect> = MarkMustAttackDefenderEffect::class

    override fun execute(
        state: GameState,
        effect: MarkMustAttackDefenderEffect,
        context: EffectContext
    ): EffectResult {
        val creatureId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        if (creatureId !in state.getBattlefield()) return EffectResult.success(state)
        val defenderId = context.resolveTarget(effect.defender, state)
            ?: return EffectResult.success(state)

        val isPlayer = defenderId in state.turnOrder
        if (!isPlayer && defenderId !in state.getBattlefield()) return EffectResult.success(state)

        val requirement = MustAttackDefenderRequirement(
            defenderId = defenderId,
            defenderGeneration = if (isPlayer) null else state.objectRef(defenderId)?.generation,
            activeOnTurn = when (effect.window) {
                AttackRequirementWindow.THIS_TURN -> state.turnNumber
                AttackRequirementWindow.CONTROLLERS_NEXT_TURN -> null
            }
        )
        val newState = state.updateEntity(creatureId) { container ->
            val existing = container.get<MustAttackDefenderComponent>()?.requirements ?: emptyList()
            container.with(MustAttackDefenderComponent(existing + requirement))
        }
        return EffectResult.success(newState)
    }
}
