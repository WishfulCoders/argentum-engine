package com.wingedsheep.replay

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId

/** The two players, by the replay's side names. */
data class Seats(val user: EntityId, val oppo: EntityId) {
    fun of(side: String): EntityId = if (side == "user") user else oppo
    fun sideOf(player: EntityId): String = if (player == user) "user" else "oppo"

    companion object {
        fun of(state: GameState): Seats {
            val byName = state.turnOrder.associateBy { id ->
                state.getEntity(id)?.get<PlayerComponent>()?.name
            }
            return Seats(requireNotNull(byName["user"]), requireNotNull(byName["oppo"]))
        }
    }
}

/** The public state the replay records at end of turn, read off an engine state. */
data class Snapshot(
    val userHand: Map<String, Int>,
    val oppoHand: Int,
    val battlefield: Map<String, Map<String, Int>>,
    val tokens: Map<String, Int>,
    val life: Map<String, Int>,
)

/**
 * Reads [Snapshot]s and compares them with the replay's. Names are canonical front-face names,
 * as in the export: a transformed double-faced permanent is reported under its front face.
 */
class Snapshotter(definitions: Iterable<CardDefinition>, private val compareTokens: Boolean = true) {
    private val frontOf: Map<String, String> =
        definitions.mapNotNull { d -> d.backFace?.let { it.name to d.name } }.toMap()

    fun canonical(name: String): String = frontOf[name] ?: name

    fun name(state: GameState, id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name?.let(::canonical)

    fun take(state: GameState, seats: Seats): Snapshot {
        val sides = listOf("user", "oppo")
        // controller, not owner: a stolen creature is on its controller's side in the replay
        fun permanents(side: String) = state.controlledBattlefield(seats.of(side))
        return Snapshot(
            userHand = counts(state.getHand(seats.user).mapNotNull { name(state, it) }),
            oppoHand = state.getHand(seats.oppo).size,
            battlefield = sides.associateWith { side ->
                counts(permanents(side).filterNot { isToken(state, it) }.mapNotNull { name(state, it) })
            },
            tokens = sides.associateWith { side -> permanents(side).count { isToken(state, it) } },
            life = sides.associateWith { side ->
                state.getEntity(seats.of(side))?.get<LifeTotalComponent>()?.life ?: 0
            },
        )
    }

    /** Human-readable differences; empty means the engine state matches the record. */
    fun diff(engine: Snapshot, record: EotSpec): List<String> {
        val out = mutableListOf<String>()
        multisetDiff("user_hand", engine.userHand, counts(record.userHand))?.let(out::add)
        if (engine.oppoHand != record.oppoHand) out += "oppo_hand ${engine.oppoHand} vs ${record.oppoHand}"
        for (side in listOf("user", "oppo")) {
            multisetDiff("battlefield.$side", engine.battlefield[side].orEmpty(),
                counts(record.battlefield[side].orEmpty()))?.let(out::add)
            val life = record.life[side]?.toInt() ?: 0
            if (engine.life[side] != life) out += "life.$side ${engine.life[side]} vs $life"
            val tokens = record.tokens[side] ?: 0
            if (compareTokens && engine.tokens[side] != tokens) out += "tokens.$side ${engine.tokens[side]} vs $tokens"
        }
        return out
    }

    private fun isToken(state: GameState, id: EntityId): Boolean =
        state.getEntity(id)?.has<TokenComponent>() == true

    companion object {
        fun counts(names: List<String>): Map<String, Int> = names.groupingBy { it }.eachCount()

        /** "label +extra -missing" relative to the record, or null when equal. */
        fun multisetDiff(label: String, engine: Map<String, Int>, record: Map<String, Int>): String? {
            val extra = (engine.keys + record.keys).flatMap { k ->
                List(maxOf(0, (engine[k] ?: 0) - (record[k] ?: 0))) { "+$k" }
            }
            val missing = (engine.keys + record.keys).flatMap { k ->
                List(maxOf(0, (record[k] ?: 0) - (engine[k] ?: 0))) { "-$k" }
            }
            if (extra.isEmpty() && missing.isEmpty()) return null
            return "$label ${(extra + missing).sorted().joinToString(" ")}"
        }
    }
}
