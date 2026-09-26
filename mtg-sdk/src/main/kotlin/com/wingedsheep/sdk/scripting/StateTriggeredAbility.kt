package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.Serializable

/**
 * A state-triggered ability per CR 603.8: it triggers whenever its [condition] becomes true,
 * rather than in response to a [GameEvent].
 *
 * Per CR 603.8:
 * - The condition is checked each time a player would receive priority.
 * - When the condition becomes true the ability goes onto the stack as a normal triggered
 *   ability and resolves under stack rules.
 * - It does not trigger again while the condition stays true (the "latch"). The engine
 *   tracks this per (entityId, abilityId) via
 *   [com.wingedsheep.engine.state.components.battlefield.StateTriggerLatchesComponent]
 *   in the rules-engine module.
 *
 * Deliberate simplification vs the letter of CR 603.8: the printed rule resets once the
 * ability *leaves the stack* (resolves / is countered) and re-triggers if the condition is
 * still true. This engine instead resets the latch when the condition next evaluates
 * *false*. The two agree for every state trigger whose effect removes the source or clears
 * the condition (the only shape shipped so far — "sacrifice this creature" cards). They
 * diverge only for a state trigger that leaves both the source and the condition intact,
 * where the printed rule would re-fire each time it resolves; no such card exists yet.
 * Revisit (reset on leaves-the-stack) before authoring one.
 *
 * Authored on cards like Dandân ("When you control no Islands, sacrifice this creature"),
 * Force Bubble ("when there are four or more depletion counters on ~, sacrifice it"), etc.
 * Use the [com.wingedsheep.sdk.dsl.CardBuilder.stateTriggeredAbility] DSL block; don't
 * instantiate directly.
 */
@Serializable
data class StateTriggeredAbility(
    val id: AbilityId,
    /** The state predicate (CR 603.8). Evaluated against the source permanent's context. */
    val condition: Condition,
    val effect: Effect,
    val activeZone: Zone = Zone.BATTLEFIELD,
    /** Optional human-readable override; otherwise auto-generated from condition + effect. */
    val descriptionOverride: String? = null
) : TextReplaceable<StateTriggeredAbility> {

    val description: String
        get() = descriptionOverride ?: buildString {
            append("when ")
            append(condition.description.removePrefix("if ").removePrefix("If "))
            append(", ")
            append(effect.description.replaceFirstChar { it.lowercase() })
            append(".")
        }

    override fun applyTextReplacement(replacer: TextReplacer): StateTriggeredAbility {
        val newCondition = condition.applyTextReplacement(replacer)
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newCondition !== condition || newEffect !== effect)
            copy(condition = newCondition, effect = newEffect) else this
    }

    companion object {
        fun create(
            condition: Condition,
            effect: Effect,
            activeZone: Zone = Zone.BATTLEFIELD,
            descriptionOverride: String? = null
        ): StateTriggeredAbility = StateTriggeredAbility(
            id = AbilityId.next(),
            condition = condition,
            effect = effect,
            activeZone = activeZone,
            descriptionOverride = descriptionOverride
        )
    }
}
