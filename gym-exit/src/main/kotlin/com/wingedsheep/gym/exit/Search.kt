package com.wingedsheep.gym.exit

import com.wingedsheep.gym.GameGymEnv
import com.wingedsheep.gym.contract.TrainingObservation
import java.util.SplittableRandom
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * mtg-draft-ai docs/65 §3 item 1 (E1): `search.py`'s PIMC search, inside the JVM.
 *
 * The algorithm is `Searcher`'s, step for step: a Gumbel / sequential-halving root over [GameplayPolicy.topActions],
 * [SearchConfig.worlds] determinized worlds per searched decision (each with its own tree, a candidate's simulations
 * rotating through them), PUCT below the root over up to k sampled complete actions plus pass, critic leaves from the
 * root seat's side and an unvisited edge's Q taken as its node's value. What differs is the engine side:
 *
 * - a node holds a [GameGymEnv.forkForSearch] fork, i.e. the immutable `GameState` and the action registry (the
 *   legal-action list), and keeps its featurized observation only until its edges are sampled;
 * - the worlds are forked and determinized once per searched decision, from the acting seat's side
 *   ([GameGymEnv.determinize] with a viewer), so either seat of a two-learner env can search;
 * - a new node's critic value and its encoder outputs come from one net pass, which [sampleEdges] reuses
 *   (`search.py` encodes the node twice);
 * - the RNG streams (world seeds, edge samples, root noise) are the JVM's, so a game is equivalent to T1's, not
 *   identical.
 *
 * One [Searcher] per game lane (thread): a node's descendants share its simulator, which is single-threaded.
 */
data class SearchConfig(
    val sims: Int,
    val worlds: Int = 4,
    val k: Int = 8,
    val cPuct: Double = 1.25,
    val cVisit: Double = 50.0,
    val cScale: Double = 1.0,
    val rootNoise: Boolean = false,
    /**
     * Run each halving phase in rounds: one simulation per live candidate, their new leaves read by the net in one
     * batch. Candidates' subtrees are disjoint and every node's edge sampler is seeded by its path, so the result is
     * identical to running each candidate's simulations back to back (false), which is `search.py`'s order.
     */
    val batchRounds: Boolean = true,
) {
    init {
        require(sims >= worlds && worlds >= 1 && k >= 1) { "need sims >= worlds >= 1 and k >= 1, got $this" }
    }
}

/** The net as the search reads it: one encoder pass per observation, batched however the implementation likes. */
fun interface Evaluator {
    fun encode(prepared: List<Prepared>): List<Encoded>
}

class Edge(
    /** Index into the node's legal actions; with [params] it is the edge's identity across forks. */
    val index: Int,
    val actionId: Int,
    val params: Params,
    var prior: Double,
    /** The policy's completed action, at the root. */
    val choice: Choice? = null,
) {
    val key: String = "$index|${params.canonical()}"
    var visits = 0
    var valueSum = 0.0
    var child: Node? = null
    var dead = false
}

class Node(
    /** The position; null once terminal. */
    var env: GameGymEnv?,
    /** The position as the net read it, until [edges] are sampled; null when the policy cannot read it. */
    var prepared: Prepared?,
    var encoded: Encoded?,
    val seat: Int,
    /** From the root seat's side. */
    val value: Double,
    /** Seeds this node's edge sampler: a function of the decision's seed, the world and the path to it. */
    val seed: Long,
    val terminal: Boolean = false,
    var edges: List<Edge>? = null,
) {
    var visits = 0
}

/** `SearchTarget`: what docs/65's fit needs from one searched decision. Values are the root seat's, in [-1, 1]. */
class SearchTarget(
    val candidates: List<Candidate>,
    val rootValue: Double,
    val chosen: Int,
    val noise: DoubleArray?,
) {
    class Candidate(val choice: Choice, val logit: Double, val visits: Int, val value: Double?)
}

class DecisionStats {
    var searched = false
    var simulations = 0
    var worldsUsed = 0
    var worldsSkipped = 0
    var deadEdges = 0
    var leafFallbacks = 0
    var rootEdges = 0
    var changedFromArgmax = false
    var nanos = 0L
    var target: SearchTarget? = null
}

class Searcher(
    private val policy: GameplayPolicy,
    private val evaluator: Evaluator,
    val config: SearchConfig,
) {
    val counts = sortedMapOf<String, Int>()
    private fun count(key: String) { counts[key] = (counts[key] ?: 0) + 1 }

    /** Where a lane's search time goes, in nanoseconds: worlds (fork + determinize), engine steps, featurize, net, edge sampling. */
    val nanos = sortedMapOf<String, Long>()
    private inline fun <T> timed(key: String, block: () -> T): T {
        val t0 = System.nanoTime()
        try { return block() } finally { nanos[key] = (nanos[key] ?: 0L) + (System.nanoTime() - t0) }
    }

    // -- the policy, as the search reads it ---------------------------------------------------------------------

    /** Prepare and encode [observations] in one batch; null where the policy cannot read one. */
    private fun read(observations: List<TrainingObservation>): List<Pair<Prepared, Encoded>?> {
        val prepared = timed("featurize") { observations.map { o -> try { policy.prepare(o) } catch (_: InferenceError) { null } } }
        val ready = prepared.filterNotNull()
        val encoded = if (ready.isEmpty()) emptyList() else timed("net") { evaluator.encode(ready) }
        var next = 0
        return prepared.map { p -> p?.let { it to encoded[next++] } }
    }

    /** Up to k distinct complete actions sampled at temperature 1, plus pass when legal; priors renormalised. */
    private fun sampleEdges(prepared: Prepared, encoded: Encoded, seed: Long): List<Edge> {
        val rng = SplittableRandom(seed)
        val edges = LinkedHashMap<String, Edge>()
        repeat(config.k) {
            val choice = try { policy.decide(prepared, encoded, Picker.sample(rng)) } catch (_: InferenceError) { return@repeat }
            val edge = Edge(choice.actionIndex, choice.actionId, choice.params, exp(choice.logProbability))
            edges.putIfAbsent(edge.key, edge)
        }
        val legal = prepared.observation.legalActions
        val pass = legal.indexOfFirst { it.kind == "PassPriority" }
        if (edges.isNotEmpty() && pass >= 0 && edges.values.none { it.index == pass }) {
            val prior = try { exp(policy.replay(prepared, encoded, intArrayOf(pass)).logProbability) } catch (_: InferenceError) { 0.0 }
            val edge = Edge(pass, legal[pass].actionId, Params(), prior)
            edges[edge.key] = edge
        }
        val total = edges.values.sumOf { it.prior }
        for (edge in edges.values) edge.prior = if (total > 0) edge.prior / total else 1.0 / edges.size
        return edges.values.toList()
    }

    /** The root's distinct candidates, each with the action head's log-probability (the halving's logit). */
    private fun rootCandidates(prepared: Prepared, encoded: Encoded): List<Pair<Edge, Double>> {
        val out = ArrayList<Pair<Edge, Double>>()
        val seen = HashSet<String>()
        for ((choice, logP) in policy.topActions(prepared, encoded, config.k)) {
            val edge = Edge(choice.actionIndex, choice.actionId, choice.params, exp(logP), choice)
            if (seen.add(edge.key)) out += edge to logP
        }
        return out
    }

    // -- one decision -------------------------------------------------------------------------------------------

    /**
     * Search [env]'s current decision ([observation] is what it shows the acting seat) and return the move to play
     * there, plus what it cost. [seed] fixes the worlds, the edge samples and the root noise for this decision.
     * Throws [InferenceError] when the policy cannot answer the decision at all, as `policy.choose` does.
     */
    fun decide(env: GameGymEnv, observation: TrainingObservation, seed: Long): Pair<Choice, DecisionStats> {
        val stats = DecisionStats()
        val t0 = System.nanoTime()
        val prepared = policy.prepare(observation)
        val encoded = evaluator.encode(listOf(prepared)).single()
        val argmax = policy.decide(prepared, encoded, Picker.argmax())
        val candidates = if (observation.legalActions.size >= 2) rootCandidates(prepared, encoded) else emptyList()
        if (candidates.size < 2) {
            stats.nanos = System.nanoTime() - t0
            return argmax to stats
        }
        stats.searched = true
        stats.rootEdges = candidates.size
        // `observation_seat`: the server's acting seat, else the perspective seat.
        val rootSeat = env.status().actingSeat
            ?: observation.players.indexOfFirst { it.id == observation.perspectivePlayerId }.coerceAtLeast(0)
        val rootPlayer = observation.perspectivePlayerId.value
        val signature = signature(observation)

        val worlds = timed("worlds") {
            (0 until config.worlds).map { w ->
                val world = env.forkForSearch()
                world to world.determinize(mix(seed, w.toLong()), rootSeat).observation as TrainingObservation
            }
        }
        val kept = worlds.filter { (_, o) -> signature(o) == signature }
        stats.worldsSkipped = worlds.size - kept.size
        val read = read(kept.map { it.second })
        val roots = kept.mapIndexed { i, (world, o) ->
            val r = read[i]
            // A root's edges are the candidates, so it never samples and keeps no observation.
            Node(world, null, null, rootSeat, r?.second?.value?.toDouble() ?: 0.0, mix(seed, 0x5eed00L + i)).also { root ->
                root.edges = candidates.map { (e, _) -> Edge(e.index, o.legalActions[e.index].actionId, e.params, e.prior) }
            }
        }
        stats.worldsUsed = roots.size
        val logits = candidates.map { it.second }
        val noise = if (config.rootNoise) {
            val g = SplittableRandom(mix(seed, 0x6a09L))
            DoubleArray(logits.size) { -ln(-ln(g.nextDouble().takeIf { it > 0 } ?: 1e-12)) }
        } else null
        val best = if (roots.isNotEmpty()) halve(logits, roots, rootSeat, rootPlayer, stats, noise) else null
        stats.nanos = System.nanoTime() - t0
        if (best == null) {
            count(if (roots.isEmpty()) "no_world" else "all_candidates_refused")
            return argmax to stats
        }
        val (index, visits, totals) = best
        val edge = candidates[index].first
        stats.changedFromArgmax = edge.key != "${argmax.actionIndex}|${argmax.params.canonical()}"
        stats.target = SearchTarget(
            candidates.mapIndexed { i, (e, logP) ->
                SearchTarget.Candidate(e.choice!!, logP, visits[i], if (visits[i] > 0) totals[i] / visits[i] else null)
            },
            rootValue = totals.sum() / maxOf(1, visits.sum()),
            chosen = index,
            noise = noise,
        )
        return edge.choice!! to stats
    }

    /**
     * Sequential halving over the root candidates: the move's index (null if every candidate was refused), and each
     * candidate's visits and value total.
     */
    private fun halve(
        logits: List<Double>, roots: List<Node>, rootSeat: Int, rootPlayer: String, stats: DecisionStats,
        noise: DoubleArray?,
    ): Triple<Int, IntArray, DoubleArray>? {
        val n = logits.size
        val visits = IntArray(n)
        val totals = DoubleArray(n)
        val dead = HashSet<Int>()
        val base = DoubleArray(n) { logits[it] + (noise?.get(it) ?: 0.0) }

        fun score(a: Int): Double {
            if (visits[a] == 0) return base[a]
            val q = (totals[a] / visits[a] + 1.0) / 2.0
            return base[a] + (config.cVisit + visits.max()) * config.cScale * q
        }

        var remaining = (0 until n).toList()
        val phases = maxOf(1, ceil(log2(n.toDouble())).toInt())
        for (phase in 0 until phases) {
            val share = maxOf(1, config.sims / (phases * remaining.size))
            val left = IntArray(n)
            for (a in remaining) left[a] = share
            fun record(a: Int, value: Double) {
                visits[a]++
                totals[a] += value
                stats.simulations++
            }
            // One simulation of candidate a, through the first world (in its rotation) that accepts the move.
            fun descendCandidate(a: Int): Descent {
                left[a]--
                for (attempt in roots.indices) {
                    val root = roots[(visits[a] + attempt) % roots.size]
                    val d = descend(root, rootSeat, rootPlayer, stats, root.edges!![a])
                    if (d !== Refused) return d
                }
                dead += a
                return Refused
            }
            if (!config.batchRounds) {
                for (a in remaining) while (left[a] > 0 && a !in dead) {
                    when (val d = descendCandidate(a)) {
                        is Done -> record(a, backup(d.path, d.value))
                        is Pending -> record(a, expand(listOf(d), rootSeat, rootPlayer, stats).single())
                        Refused -> {}
                    }
                }
            } else {
                while (true) {
                    val active = remaining.filter { left[it] > 0 && it !in dead }
                    if (active.isEmpty()) break
                    val pending = ArrayList<Pair<Int, Pending>>()
                    for (a in active) {
                        when (val d = descendCandidate(a)) {
                            is Done -> record(a, backup(d.path, d.value))
                            is Pending -> pending += a to d
                            Refused -> {}
                        }
                    }
                    if (pending.isNotEmpty()) {
                        val values = expand(pending.map { it.second }, rootSeat, rootPlayer, stats)
                        for ((i, p) in pending.withIndex()) record(p.first, values[i])
                    }
                }
            }
            remaining = remaining.filter { it !in dead }
            if (remaining.isEmpty()) return null
            if (phase < phases - 1) {
                remaining = remaining.sortedWith(compareBy<Int>({ -score(it) }, { it }))
                    .take(maxOf(1, ceil(remaining.size / 2.0).toInt()))
            }
        }
        // Python's max(): the first of equal keys, in `remaining`'s order.
        var best = remaining.first()
        for (a in remaining.drop(1)) {
            val c = compareValues(score(a), score(best)).takeIf { it != 0 } ?: compareValues(logits[a], logits[best])
            if (c > 0) best = a
        }
        return Triple(best, visits, totals)
    }

    // -- one simulation -----------------------------------------------------------------------------------------

    /** Where a simulation's descent ended. */
    private sealed interface Descent
    /** At a value already known (a terminal or a node with no live edge): back it up along [path]. */
    private class Done(val path: List<Pair<Node, Edge>>, val value: Double) : Descent
    /** At a new position: [edge] (the last of [path]) was stepped into [branch], whose [observation] the net reads. */
    private class Pending(val path: List<Pair<Node, Edge>>, val branch: GameGymEnv, val observation: TrainingObservation) : Descent
    /** The forced root edge was refused in this world. */
    private object Refused : Descent

    /**
     * `_simulate` up to the point where the net is needed: descend from [root] (through [forced] if given), stepping
     * one new edge. A terminal child is made here; any other new child is left [Pending] for [expand].
     */
    private fun descend(root: Node, rootSeat: Int, rootPlayer: String, stats: DecisionStats, forced: Edge?): Descent {
        var node = root
        val path = ArrayList<Pair<Node, Edge>>()
        while (true) {
            if (node.terminal) return Done(path, node.value)
            if (node.edges == null) {
                val p = node.prepared
                val e = node.encoded
                node.edges = if (p != null && e != null) timed("sample_edges") { sampleEdges(p, e, node.seed) } else emptyList()
                // The observation was kept for this alone.
                node.prepared = null
                node.encoded = null
            }
            val edge = if (node === root && forced != null) {
                if (forced.dead) return Refused
                forced
            } else {
                val live = node.edges!!.filter { !it.dead }
                if (live.isEmpty()) return Done(path, node.value)
                select(node, live, rootSeat)
            }
            val child = edge.child
            if (child != null) {
                path += node to edge
                node = child
                continue
            }
            val branch = node.env!!.forkForSearch()
            val result = try {
                timed("step") { branch.step(edge.actionId, edge.params.toActionParams()) }
            } catch (e: Exception) {
                edge.dead = true
                stats.deadEdges++
                count("dead_edge:${e.javaClass.simpleName}:${e.message.orEmpty().take(60)}")
                if (node === root && forced != null) return Refused
                continue
            }
            path += node to edge
            val status = branch.status()
            if (status.terminated || status.truncated) {
                val reward = if (status.truncated) 0.0 else status.reward.firstOrNull { it.playerId.value == rootPlayer }?.value ?: 0.0
                val terminal = Node(null, null, null, rootSeat, reward, childSeed(node, edge), terminal = true)
                edge.child = terminal
                return Done(path, reward)
            }
            return Pending(path, branch, result.observation as TrainingObservation)
        }
    }

    /** `_child` for every pending descent at once (one net batch), then the backups; each simulation's value. */
    private fun expand(pending: List<Pending>, rootSeat: Int, rootPlayer: String, stats: DecisionStats): List<Double> {
        val read = read(pending.map { it.observation })
        return pending.mapIndexed { i, p ->
            val (parent, edge) = p.path.last()
            val seat = p.branch.status().actingSeat ?: rootSeat
            val r = read[i]
            val child = if (r == null) {
                stats.leafFallbacks++
                Node(p.branch, null, null, seat, 0.0, childSeed(parent, edge), edges = emptyList())
            } else {
                val value = r.second.value.toDouble()
                Node(p.branch, r.first, r.second, seat, if (seat == rootSeat) value else -value, childSeed(parent, edge))
            }
            edge.child = child
            backup(p.path, child.value)
        }
    }

    private fun backup(path: List<Pair<Node, Edge>>, value: Double): Double {
        for ((parent, edge) in path) {
            edge.visits++
            edge.valueSum += value
            parent.visits++
        }
        return value
    }

    private fun childSeed(parent: Node, edge: Edge): Long = mix(parent.seed, edge.key.hashCode().toLong())

    private fun select(node: Node, live: List<Edge>, rootSeat: Int): Edge {
        val sign = if (node.seat == rootSeat) 1.0 else -1.0
        val scale = config.cPuct * sqrt(maxOf(1, node.visits).toDouble())
        var best = live.first()
        var bestScore = Double.NEGATIVE_INFINITY
        for (edge in live) {
            val q = if (edge.visits > 0) edge.valueSum / edge.visits else node.value
            val score = sign * q + scale * edge.prior / (1 + edge.visits)
            // Python's max over (score, prior): the first of equal keys.
            if (score > bestScore || (score == bestScore && edge.prior > best.prior)) {
                best = edge
                bestScore = score
            }
        }
        return best
    }

    companion object {
        /**
         * `action_signature`: what must agree between the real observation and a world's for root edges to mean the
         * same actions. `search.py` reads `kind`, `sourceId` and `cardName`, but the wire format has no `sourceId` or
         * `cardName` keys, so in T1 the check compared the action kinds alone; this keeps that.
         */
        fun signature(observation: TrainingObservation): List<String> = observation.legalActions.map { it.kind }

        /** SplitMix64 over (seed, salt): the per-world and per-purpose seeds. */
        fun mix(seed: Long, salt: Long): Long {
            var z = seed + salt * -0x61c8864680b583ebL
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
            return z xor (z ushr 31)
        }

        /** A decision's seed from its parts (deal stream, deal, decision index), as `world_seed`'s inputs. */
        fun decisionSeed(vararg parts: Long): Long = parts.fold(0x243F6A8885A308D3L) { acc, p -> mix(acc, p) }
    }
}
