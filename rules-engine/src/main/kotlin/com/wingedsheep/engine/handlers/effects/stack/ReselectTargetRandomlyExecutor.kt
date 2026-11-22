package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TargetReselectedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ReselectTargetRandomlyEffect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlin.reflect.KClass

/**
 * Executor for ReselectTargetRandomlyEffect.
 * "If it has a single target, reselect its target at random."
 *
 * Uses context.triggeringEntityId to find the spell/ability on the stack.
 * If it has exactly one target, finds all legal targets for it (via [TargetFinder]) and randomly
 * picks one.
 * If it has zero or multiple targets, does nothing.
 */
class ReselectTargetRandomlyExecutor(
    private val targetFinder: TargetFinder
) : EffectExecutor<ReselectTargetRandomlyEffect> {

    override val effectType: KClass<ReselectTargetRandomlyEffect> = ReselectTargetRandomlyEffect::class

    override fun execute(
        state: GameState,
        effect: ReselectTargetRandomlyEffect,
        context: EffectContext
    ): EffectResult {
        // 1. Get the triggering spell/ability from context
        val triggeringEntityId = context.triggeringEntityId
            ?: return EffectResult.success(state)

        val stackEntity = state.getEntity(triggeringEntityId)
            ?: return EffectResult.success(state)

        // 2. Check if it has exactly one target
        val targetsComponent = stackEntity.get<TargetsComponent>()
            ?: return EffectResult.success(state)

        val spellTargets = targetsComponent.targets
        if (spellTargets.size != 1) {
            return EffectResult.success(state)
        }

        val currentTarget = spellTargets.first()
        val requirement = targetsComponent.targetRequirements.firstOrNull()
            ?: return EffectResult.success(state)

        // 3. Find all legal targets. The object's own controller judges them ("an opponent
        // controls"); an activated/triggered ability carries no ControllerComponent.
        val spellController = TargetResolutionUtils.stackObjectController(state, triggeringEntityId)
            ?: stackEntity.get<ControllerComponent>()?.playerId
            ?: context.controllerId
        val legalTargets = findLegalTargets(state, triggeringEntityId, requirement, spellController)

        if (legalTargets.isEmpty()) {
            // No legal targets at all — keep current target
            return EffectResult.success(state)
        }

        // 4. Randomly pick one (may be the same as current — per ruling)
        val (chosenTargetId, stateAfterPick) = state.nextRandom { pick(legalTargets) }

        // 5. Build the new ChosenTarget based on what was chosen
        val newTarget = ContestedRetargetLogic.rebuildTarget(stateAfterPick, chosenTargetId, currentTarget)

        // 6. Update the target on the stack entity
        val newTargetsComponent = targetsComponent.retargeted(stateAfterPick, listOf(newTarget))
        val newState = stateAfterPick.updateEntity(triggeringEntityId) { container ->
            container.with(newTargetsComponent)
        }

        // 7. Emit event for the game log
        val spellName = stackEntity.get<CardComponent>()?.name
            ?: stackEntity.get<TriggeredAbilityOnStackComponent>()?.sourceName
            ?: stackEntity.get<ActivatedAbilityOnStackComponent>()?.sourceName
            ?: "spell or ability"
        val oldTargetId = getTargetEntityId(currentTarget)
        val newTargetId = getTargetEntityId(newTarget)
        val oldTargetName = oldTargetId?.let { resolveEntityName(state, it) } ?: "unknown"
        val newTargetName = newTargetId?.let { resolveEntityName(state, it) } ?: "unknown"
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Grip of Chaos"

        val events = if (oldTargetId != newTargetId) {
            // The ids let the client boundary mask a face-down object's name per viewer.
            listOf(TargetReselectedEvent(
                spellOrAbilityId = triggeringEntityId,
                spellOrAbilityName = spellName,
                oldTargetId = oldTargetId,
                oldTargetName = oldTargetName,
                newTargetId = newTargetId,
                newTargetName = newTargetName,
                sourceName = sourceName
            ))
        } else {
            emptyList()
        }

        return EffectResult.success(newState, events)
    }

    /**
     * All legal targets for the spell/ability's single target, the current one included.
     * Per ruling: "If there are multiple legal targets, it may still choose the original one."
     *
     * Asks [TargetFinder] for the requirement as it was chosen, judged from the object's own
     * controller and source (a spell targets as itself, an ability as its source), so the
     * requirement's filter and every targeting restriction — shroud, hexproof, protection —
     * apply exactly as when the target was first chosen. A spell or ability can't target itself.
     */
    private fun findLegalTargets(
        state: GameState,
        stackObjectId: EntityId,
        requirement: TargetRequirement,
        controllerId: EntityId
    ): List<EntityId> {
        val stackObject = state.getEntity(stackObjectId) ?: return emptyList()
        val triggered = stackObject.get<TriggeredAbilityOnStackComponent>()
        val activated = stackObject.get<ActivatedAbilityOnStackComponent>()
        val (sourceId, sourceType) = when {
            triggered != null -> triggered.sourceId to TargetingSourceType.TRIGGERED_ABILITY
            activated != null -> activated.sourceId to TargetingSourceType.ACTIVATED_ABILITY
            else -> stackObjectId to TargetingSourceType.SPELL
        }
        return targetFinder.findLegalTargets(
            state, requirement, controllerId, sourceId,
            targetingSourceType = sourceType,
            triggeringEntityId = triggered?.triggerContext?.triggeringEntityId
        ).filter { it != stackObjectId }
    }

    private fun getTargetEntityId(target: ChosenTarget): EntityId? {
        return when (target) {
            is ChosenTarget.Permanent -> target.entityId
            is ChosenTarget.Player -> target.playerId
            is ChosenTarget.Card -> target.cardId
            is ChosenTarget.Spell -> target.spellEntityId
        }
    }

    private fun resolveEntityName(state: GameState, entityId: EntityId): String {
        val entity = state.getEntity(entityId) ?: return "unknown"
        // Check if it's a card (permanent, spell, etc.)
        entity.get<CardComponent>()?.name?.let { return it }
        // Check if it's a player
        entity.get<com.wingedsheep.engine.state.components.identity.PlayerComponent>()?.let {
            return it.name
        }
        return "unknown"
    }
}
