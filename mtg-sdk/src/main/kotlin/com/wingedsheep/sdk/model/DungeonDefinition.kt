package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * A **dungeon card** (CR 309) — a nontraditional card that begins outside the game and is brought
 * into a player's command zone only by venturing into the dungeon (CR 309.2, CR 701.49).
 *
 * A dungeon is a directed graph of [rooms]: the venture marker starts on the topmost room
 * (`rooms[0]`, CR 309.4a) and moves along an arrow each time its owner ventures (CR 309.5a). A room
 * with no outgoing arrows is the bottommost room; once the marker is there and its room ability has
 * left the stack, the dungeon is removed from the game and counts as completed (CR 309.6, 309.7).
 *
 * Pure data, like a [CardDefinition]: the rooms' effects are ordinary SDK [Effect]s composed from the
 * same facades cards use, and the engine turns each into a triggered ability — "When you move your
 * venture marker into this room, [effect]" (CR 309.4c) — as the marker enters it.
 *
 * @property name The dungeon card's name ("Lost Mine of Phandelver").
 * @property rooms The rooms, topmost first. Room indices are the graph's node ids.
 * @property imageUri Scryfall image of the dungeon card, for the client's dungeon panel.
 * @property enteredOnlyByName When true, an ordinary "venture into the dungeon" can't choose this
 *   dungeon; only a "venture into [this dungeon]" can (CR 701.49d). Undercity: "You can't enter this
 *   dungeon unless you 'venture into Undercity.'"
 */
@Serializable
data class DungeonDefinition(
    val name: String,
    val rooms: List<DungeonRoom>,
    val imageUri: String? = null,
    val enteredOnlyByName: Boolean = false,
) {
    init {
        require(rooms.isNotEmpty()) { "Dungeon $name has no rooms" }
        rooms.forEachIndexed { i, room ->
            room.leadsTo.forEach { next ->
                require(next in rooms.indices && next > i) {
                    "Dungeon $name: room ${room.name} leads to $next, which isn't a later room"
                }
            }
        }
    }

    /** CR 309.4a — the room a fresh venture marker is put on. */
    val topmostRoom: Int get() = 0

    /** True for a room with no arrow pointing away from it — the dungeon's bottommost room. */
    fun isBottommost(roomIndex: Int): Boolean = rooms[roomIndex].leadsTo.isEmpty()
}

/**
 * One room of a [DungeonDefinition] (CR 309.4).
 *
 * @property name The room's name — flavor text only (CR 309.4b), but how players refer to it.
 * @property text The room ability's effect text as printed ("Scry 1.").
 * @property effect What the room ability does when it resolves.
 * @property targetRequirements The room ability's targets, chosen as it's put on the stack, like any
 *   triggered ability's (CR 603.3d).
 * @property leadsTo Indices of the rooms this room's arrows point to; empty for the bottommost room.
 */
@Serializable
data class DungeonRoom(
    val name: String,
    val text: String,
    val effect: Effect,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val leadsTo: List<Int> = emptyList(),
)
