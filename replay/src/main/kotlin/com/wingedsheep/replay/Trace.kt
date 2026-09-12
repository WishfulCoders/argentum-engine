package com.wingedsheep.replay

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import java.io.PrintStream

/**
 * Prints the search of one half-turn ([halfTurn], the 0-based index a result's `failed_at` names):
 * for each node, the step, who has priority, the stack and what the plan still expects; under it
 * every legal action, whether the plan allowed it, and for each variant tried whether the engine
 * took it or the reason it refused; and each end-of-turn comparison with the snapshot.
 *
 * Only the first [maxNodes] nodes are printed in full; end checks keep printing up to [maxLines].
 */
class Tracer(
    val halfTurn: Int,
    /** The recorded half-turn, printed as the header (the spec's JSON). */
    private val record: String,
    private val maxNodes: Int = 300,
    private val maxLines: Int = 20_000,
    private val out: PrintStream = System.out,
) {
    private var lines = 0

    fun wants(node: Int): Boolean = node <= maxNodes

    fun line(text: String) {
        if (lines++ < maxLines) out.println(text)
    }

    fun begin(spec: GameSpec, index: Int, beam: Int) {
        val ht = spec.halfTurns[index]
        line("=== ${spec.gameId} half-turn $index/${spec.halfTurns.size}: ${ht.active} turn ${ht.turn}" +
            "${if (ht.last) " (last)" else ""}, $beam start state(s)")
        line("record: $record")
    }

    fun header(n: Int, s: GameState, seats: Seats, snapshotter: Snapshotter, plan: String): String {
        val prio = s.priorityPlayerId?.let(seats::sideOf) ?: "-"
        val stack = s.stack.map { snapshotter.name(s, it) ?: "?" }
        val pending = s.pendingDecision?.let { " pending ${it::class.simpleName} for ${seats.sideOf(it.playerId)}" } ?: ""
        // the priority player's hand and floating mana: why a planned spell is or is not offered
        val holder = s.priorityPlayerId
        val hand = holder?.let { p -> s.getHand(p).map { snapshotter.name(s, it) ?: "?" } }.orEmpty()
        val pool = holder?.let { s.getEntity(it)?.get<ManaPoolComponent>() }?.let { m ->
            "WUBRGC".toList().zip(listOf(m.white, m.blue, m.black, m.red, m.green, m.colorless))
                .filter { it.second > 0 }.joinToString("") { (c, k) -> "$c".repeat(k) } +
                if (m.restrictedMana.isNotEmpty()) "+${m.restrictedMana.size}r" else ""
        }.orEmpty()
        return "#$n turn ${s.turnNumber} ${s.step} prio $prio stack $stack plan [$plan]$pending" +
            " | hand $hand${if (pool.isNotEmpty()) " pool $pool" else ""}"
    }

    fun end(nodes: Int, ends: Int, closest: List<String>?) {
        line("=== $nodes nodes, $ends matching end state(s)" +
            if (ends == 0) "; closest: ${closest?.joinToString("; ")}" else "")
        out.flush()
    }
}
