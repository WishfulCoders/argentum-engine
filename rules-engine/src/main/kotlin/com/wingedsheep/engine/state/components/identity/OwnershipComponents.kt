package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Who owns this entity. Fixed for the whole game except under the ante cards — the only cards that
 * can change a card's owner (CR 407.3) — which go through `ChangeOwnershipExecutor`; that keeps
 * [CardComponent.ownerId] and the owner-keyed zone in step and stamps [OriginalOwnerComponent].
 */
@Serializable
data class OwnerComponent(
    val playerId: EntityId
) : Component

/**
 * Who controls this entity (can change via effects).
 */
@Serializable
data class ControllerComponent(
    val playerId: EntityId
) : Component

/**
 * The player who owned this card when the game began, stamped only once an ante card has changed
 * its owner (CR 407.3) and removed again if it comes home. Ante is played "for keeps" (CR 407.1):
 * when the game ends, a card whose final owner differs from this one has changed hands for real,
 * which is what `AnteSettlement` reports to the server.
 */
@Serializable
data class OriginalOwnerComponent(
    val playerId: EntityId
) : Component
