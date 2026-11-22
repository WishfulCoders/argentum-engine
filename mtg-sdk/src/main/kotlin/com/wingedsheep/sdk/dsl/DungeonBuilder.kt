package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.model.DungeonDefinition
import com.wingedsheep.sdk.model.DungeonRoom
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Build a [DungeonDefinition] (CR 309). Rooms are declared topmost first, and each names the rooms
 * its arrows lead to; the builder resolves those names to indices.
 *
 * ```kotlin
 * val LostMineOfPhandelver = dungeon("Lost Mine of Phandelver") {
 *     room("Cave Entrance", "Scry 1.", leadsTo = listOf("Goblin Lair", "Mine Tunnels")) {
 *         effect = Effects.Scry(1)
 *     }
 *     room("Storeroom", "Put a +1/+1 counter on target creature.", leadsTo = listOf("Temple of Dumathoin")) {
 *         val creature = target(TargetFilter.Creature)
 *         effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
 *     }
 *     …
 * }
 * ```
 */
fun dungeon(name: String, init: DungeonBuilder.() -> Unit): DungeonDefinition =
    DungeonBuilder(name).apply(init).build()

@CardDsl
class DungeonBuilder(private val name: String) {
    /** Scryfall image of the dungeon card. */
    var imageUri: String? = null

    /** See [DungeonDefinition.enteredOnlyByName]. */
    var enteredOnlyByName: Boolean = false

    private data class PendingRoom(val name: String, val text: String, val leadsTo: List<String>, val builder: DungeonRoomBuilder)

    private val rooms = mutableListOf<PendingRoom>()

    /** Declare the next room down. An empty [leadsTo] marks the bottommost room. */
    fun room(name: String, text: String, leadsTo: List<String> = emptyList(), init: DungeonRoomBuilder.() -> Unit) {
        rooms += PendingRoom(name, text, leadsTo, DungeonRoomBuilder().apply(init))
    }

    fun build(): DungeonDefinition {
        val indexByName = rooms.withIndex().associate { (i, room) -> room.name to i }
        return DungeonDefinition(
            name = name,
            rooms = rooms.map { room ->
                DungeonRoom(
                    name = room.name,
                    text = room.text,
                    effect = requireNotNull(room.builder.effect) { "Room ${room.name} of $name has no effect" },
                    targetRequirements = room.builder.requirements,
                    leadsTo = room.leadsTo.map { next ->
                        requireNotNull(indexByName[next]) { "Room ${room.name} of $name leads to unknown room $next" }
                    },
                )
            },
            imageUri = imageUri,
            enteredOnlyByName = enteredOnlyByName,
        )
    }
}

/** One room's ability: its targets (declared with `target(…)`) and its [effect]. */
@CardDsl
class DungeonRoomBuilder(
    private val declaredTargets: TargetList = TargetList(),
) : TargetDeclarations by declaredTargets {
    var effect: Effect? = null

    internal val requirements get() = declaredTargets.requirements
}
