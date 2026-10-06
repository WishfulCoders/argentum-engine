package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Chokepoint for *ad-hoc* battlefield insertions (token creation, land play, permanent-
 * spell resolution, returns from linked exile, etc.).
 *
 * Wraps [GameState.addToZone] for the controller's battlefield and fires the per-turn
 * [PermanentEntryTracker] so that "an X entered the battlefield this turn" conditions
 * stay in sync — calling `addToZone(battlefieldZone, …)` directly from such a path bypasses
 * the tracker and is the recurring source of subtle "card silently can't satisfy its own
 * condition" bugs. Use this helper instead.
 *
 * The standard zone-change pipeline ([ZoneTransitionService.moveToZone]) does **not** route
 * through here: it has to record *after* [ZoneTransitionService.applyBattlefieldEntry] wires
 * the controller, so it calls [PermanentEntryTracker.record] directly instead.
 *
 * The entity must already carry the components that define its identity at entry
 * (`CardComponent`, `ControllerComponent`, etc.) — the tracker reads projected types
 * after the entity is in the zone.
 *
 * [BattlefieldEntry.place] does **not** run ETB replacement effects, set tapped state,
 * apply enters-with-counters, or emit a `ZoneChangeEvent`. Those are the caller's job
 * (or [ZoneTransitionService.moveToZone]'s, for the standard zone-change pipeline).
 */
object BattlefieldEntry {

    /**
     * Add [entityId] to [controllerId]'s battlefield and record the ETB-by-type.
     *
     * A card that was also a creature only because it wasn't on the battlefield (Grist, the Hunger
     * Tide — CR 113.6c) gets its printed characteristics back first, so an ad-hoc return (from
     * linked exile, say) doesn't put it onto the battlefield as a creature and the ETB-by-type
     * record sees what actually entered.
     */
    fun place(state: GameState, controllerId: EntityId, entityId: EntityId): GameState {
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        val printed = if (state.getEntity(entityId)
                ?.has<com.wingedsheep.engine.state.components.identity.OffBattlefieldCharacteristicsComponent>() == true
        ) {
            state.updateEntity(entityId) {
                com.wingedsheep.engine.state.components.identity.OffBattlefieldCharacteristics.enterBattlefield(it)
            }
        } else {
            state
        }
        val withZone = printed.addToZone(battlefieldZone, entityId)
        return PermanentEntryTracker.record(withZone, controllerId, entityId)
    }
}
