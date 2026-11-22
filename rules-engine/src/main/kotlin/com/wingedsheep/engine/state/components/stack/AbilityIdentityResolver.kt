package com.wingedsheep.engine.state.components.stack

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.faceDownDisplayName
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AbilityIdentity

/**
 * Transitional trigger-path derivation that pairs [abilityId] with [sourceId]'s current card
 * definition.
 *
 * This says only how the key was derived; it does not prove that the trigger belongs to that card
 * definition. [com.wingedsheep.engine.event.TriggerAbilityResolver] can also return granted and
 * synthesized triggers, so those branches need typed provenance before trigger identity is fully
 * authoritative. Activated abilities do not use this helper: their lookup records definition
 * ownership explicitly at activation time.
 *
 * Returns `null` when [sourceId] has no [CardComponent], and when it is a face-down object whose
 * card is hidden: keying a face-down permanent's ward by the card underneath would carry that
 * card's identity onto the stack, and a persistent yield or batched answer keyed by it would let
 * the hidden card change what the game does for a player who may not look at it.
 */
fun GameState.triggerIdentityFromCurrentCardDefinition(
    sourceId: EntityId,
    abilityId: AbilityId,
): AbilityIdentity? {
    if (faceDownDisplayName(this, sourceId) != null) return null
    return getEntity(sourceId)?.get<CardComponent>()?.cardDefinitionId
        ?.let { AbilityIdentity(it, abilityId) }
}
