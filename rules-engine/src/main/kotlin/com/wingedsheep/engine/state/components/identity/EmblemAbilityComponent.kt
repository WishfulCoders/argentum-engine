package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import kotlinx.serialization.Serializable

/** Activated abilities granted dynamically by a permanent emblem. */
@Serializable
data class EmblemActivatedAbilityComponent(
    val filter: GroupFilter,
    val abilities: List<ActivatedAbility>,
) : Component

/**
 * Static abilities the emblem *itself* has, as though printed on it — for emblem text that reads on
 * its controller rather than on a group of permanents ("You may cast spells from your hand without
 * paying their mana costs", Tamiyo, Field Researcher's −7).
 *
 * The synthetic emblem entity is never registered in a zone, so scans that walk the battlefield
 * looking for a printed static won't see it; a scan that should honor an emblem consults this
 * component alongside the battlefield (see
 * [com.wingedsheep.engine.mechanics.mana.CostCalculator.hasFreeCastPermission]).
 */
@Serializable
data class EmblemStaticAbilityComponent(
    val abilities: List<StaticAbility>,
) : Component

/**
 * Every static ability on an emblem [playerId] controls, paired with the emblem entity that has it
 * — the emblem half of a "does this player have permission X" scan. An emblem is always controlled
 * by the player who got it, so its statics read exactly like those of a permanent that player
 * controls (Wrenn and Realmbreaker's "You may play lands and cast permanent spells from your
 * graveyard" is [com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard] plus
 * [com.wingedsheep.sdk.scripting.MayCastFromGraveyard], the statics Crucible of Worlds and its kin
 * print). The emblem id stands in for the source permanent wherever a scan keys per-source state
 * off it, such as a `oncePerTurn` marker.
 */
fun GameState.emblemStaticAbilitiesOf(
    playerId: EntityId
): List<Pair<EntityId, StaticAbility>> {
    val result = mutableListOf<Pair<EntityId, StaticAbility>>()
    for ((entityId, container) in entities) {
        val statics = container.get<EmblemStaticAbilityComponent>() ?: continue
        if (container.get<ControllerComponent>()?.playerId != playerId) continue
        for (ability in statics.abilities) result.add(entityId to ability)
    }
    return result
}

/**
 * The battlefield visit of the permanent whose ability created this emblem — the object an emblem's
 * rules text names when it says "cards exiled with [that permanent]" (Tibalt, Cosmic Impostor:
 * "You may play cards exiled with Tibalt, Cosmic Impostor, …").
 *
 * An emblem has no linked-exile pile of its own, so a
 * [com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile] in its
 * [EmblemStaticAbilityComponent] reads the pile of the object recorded here. The object is a
 * *visit*, not an entity id (CR 400.7): the emblem keeps working after that permanent has left the
 * battlefield (the 2021-02-05 Tibalt ruling — "even after that Tibalt leaves the battlefield"), and it
 * never covers cards a later visit of the same card exiles, since that is a different object with
 * an emblem of its own. Pile lookups go through
 * [com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileLookup.exiledCardsOfVisit].
 *
 * Recorded by `CreatePermanentEmblemExecutor` only for an emblem that owns such a grant, so every
 * other emblem's state is unchanged.
 *
 * @property sourceId the creating permanent's entity id.
 * @property battlefieldTimestamp that permanent's battlefield-entry timestamp for the visit that
 *   created the emblem.
 */
@Serializable
data class EmblemLinkedSourceComponent(
    val sourceId: EntityId,
    val battlefieldTimestamp: Long,
) : Component
