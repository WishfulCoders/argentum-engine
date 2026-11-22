package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.KeywordActionReplacements
import com.wingedsheep.engine.handlers.effects.ReplaceableKeywordAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConniveEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EmitConnivedEventEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlin.reflect.KClass

/**
 * Executor for [ConniveEffect] — "[subject] connives" (CR 701.50).
 *
 * The connive proper is [ConniveEffect.body], the ordinary draw → discard → conditional +1/+1
 * counter pipeline the SDK composes; this executor adds only the two things the wrapper exists for.
 *
 * **Replacement (CR 614).** Applicable
 * [com.wingedsheep.sdk.scripting.ModifyKeywordAction] replacements on the battlefield are consulted
 * through [KeywordActionReplacements], exactly as [ExploreEffectExecutor] does for explore — connive
 * isn't dispatched as a generic replaceable event, so it's checked here directly. On a match the
 * connive is re-issued as `Composite(prefixEffects…, ConniveEffect(sameSubject,
 * replacementsApplied = true))` through the registry [recurse] runner, so:
 *  - the prefix runs *before* the connive, as printed ("instead you draw a card, then that creature
 *    connives") — which matters, because the extra card is in hand before the discard is chosen;
 *  - the composite executor's pause-sequencing carries the connive's own discard decision;
 *  - `replacementsApplied` stops the replacement applying to its own re-issue (CR 614.5), while two
 *    separate sources each still contribute their prefix.
 *
 * **Connive N (CR 701.50d/e).** [ConniveEffect.count] is evaluated only for the zero check: a
 * connive 0 is no connive at all. The body computes and stores N itself.
 *
 * **Observation (CR 701.50f).** [EmitConnivedEventEffect] is appended as the pipeline's last step
 * rather than emitted inline here: connive pauses mid-way for the discard choice, and an event
 * emitted in a paused batch does not reliably fire watcher triggers. Because it is a pipeline step
 * it also fires when the draw or the discard was impossible, which is what CR 701.50f requires.
 *
 * The subject is resolved to a concrete entity up front and re-bound as
 * [EffectTarget.SpecificEntity], so the re-issue and the tail event both refer to the permanent that
 * was conniving when the action began, not to whatever `Self`/`TriggeringEntity` would resolve to
 * after the pause.
 *
 * @param recurse registry entry point for delegating the Composite (handed to [PermanentExecutors]
 *   by the registry at construction).
 */
class ConniveEffectExecutor(
    private val recurse: (GameState, Effect, EffectContext) -> EffectResult,
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<ConniveEffect> {

    override val effectType: KClass<ConniveEffect> = ConniveEffect::class

    override fun execute(
        state: GameState,
        effect: ConniveEffect,
        context: EffectContext
    ): EffectResult {
        // CR 701.50e: a permanent that would connive 0 doesn't connive — nothing is drawn or
        // discarded, no connive replacement applies and no connive event fires. Plain connive is a
        // fixed 1, so only connive N ever reaches the evaluator.
        if (effect.count != DynamicAmount.Fixed(1) &&
            predicateEvaluator.amounts.evaluate(state, effect.count, context) <= 0
        ) {
            return EffectResult.success(state)
        }

        // A creature that convoked the resolving spell and has since left the battlefield still
        // connives, as the object it last was (CR 701.50b — Lethal Scheme).
        departedConvoker(state, effect.subject, context)?.let { (departedId, lastKnown) ->
            return conniveByLastKnownInformation(state, effect, context, departedId, lastKnown)
        }

        // Any other unresolvable subject (a targeted permanent that left before the connive ran)
        // still draws and discards — the bare pipeline, with the counter going nowhere. It has no
        // last-known information captured to match a replacement or a connive trigger against.
        val connivingId = context.resolveTarget(effect.subject, state)
            ?: return recurse(state, effect.body, context)
        val subject = EffectTarget.SpecificEntity(connivingId)

        if (!effect.replacementsApplied) {
            val prefixEffects = KeywordActionReplacements.collectPrefixes(
                state, connivingId, ReplaceableKeywordAction.CONNIVE,
                predicateEvaluator = predicateEvaluator
            )
            if (prefixEffects.isNotEmpty()) {
                val composite = CompositeEffect(
                    prefixEffects + effect.copy(subject = subject, replacementsApplied = true)
                )
                return recurse(state, composite, context)
            }
        }

        return recurse(
            state,
            CompositeEffect(listOf(effect.body, EmitConnivedEventEffect(subject))),
            context
        )
    }

    /**
     * The departed convoker [subject] names, with its last-known information — when the resolving
     * spell recorded one for it (`SpellOnStackComponent.convokedLastKnown`, stamped as it left).
     * Only a loop over [com.wingedsheep.sdk.scripting.effects.CardSource.CreaturesThatConvokedSource]
     * hands connive such a creature, so only [EffectTarget.IterationEntity] (and this executor's own
     * [EffectTarget.SpecificEntity] re-issue) is consulted.
     */
    private fun departedConvoker(
        state: GameState,
        subject: EffectTarget,
        context: EffectContext
    ): Pair<EntityId, EntitySnapshot>? {
        val id = when (subject) {
            EffectTarget.IterationEntity -> context.iterationEntityId
            is EffectTarget.SpecificEntity -> subject.entityId
            else -> null
        } ?: return null
        val departed = context.sourceId?.let { state.getEntity(it) }
            ?.get<SpellOnStackComponent>()?.convokedLastKnown
            ?: return null
        return departed[id]?.let { id to it }
    }

    /**
     * CR 701.50b: a permanent no longer on the battlefield as it is instructed to connive still
     * connives — its last known information decides which object connived and who controlled it.
     * That player draws and discards; the +1/+1 counter has nowhere to go; a replacement and a
     * "whenever … connives" trigger match the object as it last existed.
     *
     * The loop's binding is dropped for the body, so [EffectTarget.IterationEntity] — the counter's
     * recipient — names nothing rather than the card the convoker became in its new zone.
     */
    private fun conniveByLastKnownInformation(
        state: GameState,
        effect: ConniveEffect,
        context: EffectContext,
        departedId: EntityId,
        lastKnown: EntitySnapshot
    ): EffectResult {
        val departedContext = context.copy(
            controllerId = lastKnown.controllerId ?: context.controllerId,
            objectReferences = context.objectReferences.copy(iteration = null)
        )
        val subject = EffectTarget.SpecificEntity(departedId)
        if (!effect.replacementsApplied) {
            val prefixEffects = KeywordActionReplacements.collectPrefixes(
                state, departedId, ReplaceableKeywordAction.CONNIVE,
                predicateEvaluator = predicateEvaluator,
                lastKnown = lastKnown
            )
            if (prefixEffects.isNotEmpty()) {
                val composite = CompositeEffect(
                    prefixEffects + effect.copy(subject = subject, replacementsApplied = true)
                )
                return recurse(state, composite, departedContext)
            }
        }
        return recurse(
            state,
            CompositeEffect(listOf(effect.body, EmitConnivedEventEffect(subject))),
            departedContext
        )
    }
}
