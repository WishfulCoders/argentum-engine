package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Marks an entity as a **dungeon card in its owner's command zone** (CR 309.2b) — the object that is
 * the source of that dungeon's room abilities (CR 309.4c), named on the stack and checked by the
 * dungeon-completion state-based action (CR 704.5t).
 *
 * Like an emblem's synthetic source, the entity is not listed in a zone: a dungeon card is not a
 * permanent, can't be cast and can't leave the command zone except by leaving the game (CR 309.2c),
 * so nothing that scans zones for cards should ever find it. Where the marker is lives on the owner's
 * [com.wingedsheep.engine.state.components.player.DungeonProgressComponent]; this is only the card.
 *
 * @property dungeon The dungeon's name, a key into [com.wingedsheep.sdk.dungeons.Dungeons].
 * @property ownerId The player who owns it — and so controls its room abilities (CR 309.4c).
 */
@Serializable
data class DungeonCardComponent(
    val dungeon: String,
    val ownerId: EntityId
) : Component
