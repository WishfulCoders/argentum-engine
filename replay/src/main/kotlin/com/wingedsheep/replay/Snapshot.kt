package com.wingedsheep.replay

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
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

    /**
     * The card's name as 17Lands logs it: the front face, and for a copy (Omni-Changeling entering
     * as a copy of another creature) the printed card, not the one it copies.
     */
    fun name(state: GameState, id: EntityId): String? {
        val e = state.getEntity(id) ?: return null
        val copy = e.get<CopyOfComponent>()
        val printed = copy?.let { it.originalCardComponent?.name ?: it.originalCardDefinitionId }
        return (printed ?: e.get<CardComponent>()?.name)?.let(::canonical)
    }

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

    /**
     * What the snapshot cannot see about the battlefield: which non-land permanents are tapped,
     * what each aura or equipment is attached to, what a copy copies, counters, and the choices made as a permanent was
     * cast or entered (a chosen colour decides what mana a land makes). Two lines that both match a
     * snapshot but differ here (an aura on the wrong creature) diverge only turns later, so the
     * beam keeps one of each ([Reconstructor]). Tapped lands are left out: they untap next turn and
     * would otherwise flood the beam with land-order variants.
     */
    fun hidden(state: GameState, seats: Seats): List<String> =
        listOf("user", "oppo").flatMap { side ->
            state.controlledBattlefield(seats.of(side)).map { id ->
                val e = state.getEntity(id)
                val land = state.projectedState.hasType(id, "LAND")
                buildString {
                    append(side).append(':').append(name(state, id))
                    if (!land && e?.has<TappedComponent>() == true) append(" tapped")
                    e?.get<CopyOfComponent>()?.let { append(" copying ").append(e.get<CardComponent>()?.name) }
                    e?.get<AttachedToComponent>()?.let { a ->
                        append(" on ").append(name(state, a.targetId))
                        state.getEntity(a.targetId)?.get<ControllerComponent>()?.let { append('/').append(seats.sideOf(it.playerId)) }
                    }
                    e?.get<CountersComponent>()?.counters?.filterValues { it != 0 }?.takeIf { it.isNotEmpty() }
                        ?.let { append(' ').append(it.entries.sortedBy { c -> c.key.toString() }) }
                    e?.get<CastChoicesComponent>()?.let { c ->
                        c.x?.let { append(" X=").append(it) }
                        for ((slot, v) in c.chosen.entries.sortedBy { it.key.toString() }) {
                            append(' ').append(slot).append('=').append(
                                if (v is ChoiceValue.EntityChoice) name(state, v.entityId) else v.toString()
                            )
                        }
                    }
                }
            }
        }.sorted()

    /** Every battlefield object by side, tokens marked, for the trace. */
    fun board(state: GameState, seats: Seats): String =
        listOf("user", "oppo").joinToString(" | ") { side ->
            "$side: " + state.controlledBattlefield(seats.of(side)).joinToString(", ") { id ->
                (name(state, id) ?: "<no card ${id.value}>") + if (isToken(state, id)) " (token)" else ""
            }
        }

    fun isToken(state: GameState, id: EntityId): Boolean =
        state.getEntity(id)?.has<TokenComponent>() == true

    /**
     * Whether entity [id] is the card the replay calls [key]. Combat lists name tokens as
     * `token:<name>` (17Lands `cards.csv` token names); anything else is a non-token card name.
     */
    fun matches(state: GameState, id: EntityId, key: String): Boolean {
        val name = name(state, id) ?: return false
        return if (key.startsWith(TOKEN_PREFIX)) {
            isToken(state, id) && name.removeSuffix(" Token") == key.removePrefix(TOKEN_PREFIX)
        } else {
            !isToken(state, id) && name == key
        }
    }

    companion object {
        const val TOKEN_PREFIX = "token:"

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
