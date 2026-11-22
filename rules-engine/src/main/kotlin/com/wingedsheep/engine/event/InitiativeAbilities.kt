package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.InitiativeTakenEvent
import com.wingedsheep.engine.mechanics.initiative.InitiativeService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.PlayerDesignation
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dungeons.Dungeons
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The three inherent triggered abilities of having the initiative (CR 726.2):
 *
 * - "At the beginning of the upkeep of the player who has the initiative, that player ventures into
 *   Undercity."
 * - "Whenever one or more creatures a player controls deal combat damage to the player who has the
 *   initiative, the controller of those creatures takes the initiative."
 * - "Whenever a player takes the initiative, that player ventures into Undercity."
 *
 * Like the monarch's ([MonarchAbilities]) they have **no source** and are controlled by whoever had
 * the initiative when they triggered, so they are ordinary [TriggeredAbility]s whose `sourceId` is
 * the holder's player entity, synthesized by [TriggerDetector] for [GameState.initiative] only. They
 * use the stack like any trigger and can be responded to.
 *
 * The [AbilityId]s are stable constants: pending triggers are keyed by ability identity, so the copies
 * the detector hands out must compare equal.
 */
object InitiativeAbilities {

    val UPKEEP_VENTURE_ID: AbilityId = AbilityId("inherent_initiative_upkeep_venture")
    val COMBAT_DAMAGE_STEAL_ID: AbilityId = AbilityId("inherent_initiative_combat_damage_steal")
    val TAKE_VENTURE_ID: AbilityId = AbilityId("inherent_initiative_take_venture")

    private val ventureIntoUndercity = Effects.VentureIntoTheDungeon(EffectTarget.Controller, into = Dungeons.Undercity.name)

    /** "At the beginning of the upkeep of the player who has the initiative, that player ventures into Undercity." */
    val upkeepVenture: TriggeredAbility = TriggeredAbility(
        id = UPKEEP_VENTURE_ID,
        trigger = EventPattern.StepEvent(Step.UPKEEP, Player.You),
        binding = TriggerBinding.ANY,
        effect = ventureIntoUndercity,
        descriptionOverride = "At the beginning of the upkeep of the player who has the initiative, " +
            "that player ventures into Undercity."
    )

    /**
     * "Whenever one or more creatures a player controls deal combat damage to the player who has the
     * initiative, the controller of those creatures takes the initiative."
     *
     * Batched per attacking player (CR 603.2c — "one or more"): two players' creatures connecting in
     * the same damage step give the holder two triggers to order, one per player, and the last to
     * resolve keeps the initiative. The new holder is carried as the triggering player, read with
     * last-known information, so a creature that died in the same damage step still counts for the
     * player who controlled it. Detected by [detectCombatDamageSteals], not by the pattern below.
     */
    val combatDamageSteal: TriggeredAbility = TriggeredAbility(
        id = COMBAT_DAMAGE_STEAL_ID,
        trigger = EventPattern.DesignationTakenEvent(PlayerDesignation.INITIATIVE, Player.You),
        binding = TriggerBinding.ANY,
        effect = Effects.TakeTheInitiative(EffectTarget.PlayerRef(Player.TriggeringPlayer)),
        descriptionOverride = "Whenever one or more creatures a player controls deal combat damage to " +
            "the player who has the initiative, the controller of those creatures takes the initiative."
    )

    /** "Whenever a player takes the initiative, that player ventures into Undercity." */
    val takeVenture: TriggeredAbility = TriggeredAbility(
        id = TAKE_VENTURE_ID,
        trigger = EventPattern.DesignationTakenEvent(PlayerDesignation.INITIATIVE, Player.You),
        binding = TriggerBinding.ANY,
        effect = ventureIntoUndercity,
        descriptionOverride = "Whenever a player takes the initiative, that player ventures into Undercity."
    )

    /**
     * The take-venture trigger for every [InitiativeTakenEvent] in [events]. "That player" is the
     * one who took it, and they are also the one who has it as it triggers, so they control it.
     */
    fun detectTakeVentures(events: List<GameEvent>, triggers: MutableList<PendingTrigger>) {
        for (event in events) {
            if (event !is InitiativeTakenEvent) continue
            val holder = event.newHolderId
            triggers += PendingTrigger(
                ability = takeVenture,
                sourceId = holder,
                sourceName = InitiativeService.SOURCE_NAME,
                controllerId = holder,
                triggerContext = TriggerContext(triggeringPlayerId = holder)
            )
        }
    }

    /**
     * The combat-damage steal for the batch [events] (CR 726.2): one trigger per player whose
     * creatures dealt combat damage to the player who has the initiative, controlled by that holder.
     */
    fun detectCombatDamageSteals(state: GameState, events: List<GameEvent>, triggers: MutableList<PendingTrigger>) {
        val holder = state.initiative ?: return
        val stealers = linkedSetOf<EntityId>()
        for (event in events) {
            if (event !is DamageDealtEvent || !event.isCombatDamage || event.targetId != holder) continue
            val sourceId = event.sourceId ?: continue
            controllerOfDamagingCreature(state, sourceId)?.let { stealers += it }
        }
        for (stealer in stealers) {
            triggers += PendingTrigger(
                ability = combatDamageSteal,
                sourceId = holder,
                sourceName = InitiativeService.SOURCE_NAME,
                controllerId = holder,
                triggerContext = TriggerContext(triggeringPlayerId = stealer)
            )
        }
    }

    /**
     * Who controlled the creature that dealt the damage — its current controller, or, if combat
     * damage already killed it, its last-known controller (CR 608.2h), falling back to its owner.
     * Only creatures deal combat damage, so no type check is needed.
     */
    private fun controllerOfDamagingCreature(state: GameState, sourceId: EntityId): EntityId? {
        val container = state.getEntity(sourceId) ?: return null
        return state.projectedState.getController(sourceId)
            ?: container.get<ControllerComponent>()?.playerId
            ?: container.get<LastKnownPermanentComponent>()?.snapshot?.controllerId
            ?: container.get<CardComponent>()?.ownerId
    }
}
