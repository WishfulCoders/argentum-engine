package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.MassAnimateEffect
import kotlin.reflect.KClass

/**
 * Executor for [MassAnimateEffect].
 *
 * Captures every permanent matching the filter against the *current* battlefield (CR 611.2c —
 * the set of affected permanents is locked in at resolution time), then layers floating
 * continuous effects keyed to that set for [MassAnimateEffect.duration]:
 *   - Layer 4 (TYPE): AddType("CREATURE")
 *   - Layer 6 (ABILITY): RemoveAllAbilities (when requested)
 *   - Layer 7b (POWER_TOUGHNESS, SET_VALUES): base P/T = the effect's [MassAnimateEffect.power] /
 *     [MassAnimateEffect.toughness] dynamic amounts, evaluated per affected entity
 *
 * The dynamic P/T is resolved per affected permanent, so a formula like
 * `EntityProperty(AffectedEntity, ManaValue)` gives each animated permanent its own mana value —
 * not the source's. This is the one-shot "this effect continues until end of turn" companion to the
 * continuous group statics (GrantCardType + LoseAllAbilities + SetBasePowerToughnessDynamicStatic)
 * used while the generating permanent (e.g. Titania's Song) is on the battlefield.
 */
class MassAnimateExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<MassAnimateEffect> {

    override val effectType: KClass<MassAnimateEffect> = MassAnimateEffect::class

    override fun execute(
        state: GameState,
        effect: MassAnimateEffect,
        context: EffectContext
    ): EffectResult {
        val affectedEntities = BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, effect.filter, context,
            predicateEvaluator = predicateEvaluator
        ).toSet()

        if (affectedEntities.isEmpty()) {
            return EffectResult.success(state)
        }

        // Layer 4 (TYPE): becomes a creature.
        var newState = state.addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.AddType("CREATURE"),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        // Layer 6 (ABILITY): loses all abilities.
        if (effect.loseAllAbilities) {
            newState = newState.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.RemoveAllAbilities,
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        // Layer 7b (POWER_TOUGHNESS, SET_VALUES): base P/T = the effect's dynamic amounts,
        // resolved per affected entity.
        newState = newState.addFloatingEffect(
            layer = Layer.POWER_TOUGHNESS,
            sublayer = Sublayer.SET_VALUES,
            modification = SerializableModification.SetPowerToughnessDynamic(effect.power, effect.toughness),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
