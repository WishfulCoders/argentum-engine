package com.wingedsheep.engine.event

import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import kotlinx.serialization.Serializable

/**
 * An activated ability that has been granted to an entity temporarily.
 *
 * Used for effects like Run Wild that grant activated abilities
 * until end of turn. Stored in GameState.grantedActivatedAbilities
 * and checked by GameSession when computing legal actions and by
 * ActivateAbilityHandler when validating/executing activations.
 *
 * @property entityId The entity that has the granted ability
 * @property ability The activated ability that was granted
 * @property duration How long the grant lasts
 * @property sourceId The permanent whose effect handed out the grant, mirroring
 *   [com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect.sourceId]. Required for
 *   source-keyed "for as long as …" durations ([Duration.WhileSourceOnBattlefield] and friends):
 *   without it `EndedDurationExpiryCheck` cannot tell when the gate has closed, and the grant would
 *   outlive its source (Kitesail Larcenist's Treasure mana ability surviving Kitesail's death).
 *   Null for grants whose duration needs no source (`Permanent`, `EndOfTurn`, affected-keyed gates).
 * @property sourceObject The exact rules object the source was when the grant was made — set only
 *   for [Duration.UntilSourceCastFromExile], which ends when *that* exiled object is cast. A source
 *   that leaves exile any other way becomes a new object (CR 400.7) and no later cast matches, so
 *   the grant lasts indefinitely; null (the source wasn't in exile at grant time) means the same.
 */
@Serializable
data class GrantedActivatedAbility(
    val entityId: EntityId,
    val ability: ActivatedAbility,
    val duration: Duration,
    val sourceId: EntityId? = null,
    val sourceObject: ObjectRef? = null
)
