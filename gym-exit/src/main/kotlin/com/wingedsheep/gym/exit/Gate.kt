package com.wingedsheep.gym.exit

import com.wingedsheep.gym.contract.Observation
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.gym.service.EnvConfig
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.SplittableRandom
import java.util.zip.GZIPOutputStream

/**
 * docs/65 §3 item 4: `scripts/search_t1.py`'s game loop, in the JVM. Plays T1's deals (the env configs
 * `scripts/e1_deals.py` writes from `deals.jsonl`) with one arm and writes one row per game in T1's format to
 * `OUT/<arm>/lane<i>.jsonl`, resumable, so the same deals can be compared deal by deal.
 *
 * Arms: `A0g` (argmax of every head), `A0` (sampled at T = 1) and `S<n>` (the search at n simulations). Lanes are
 * threads in this JVM, each with its own envs and [Searcher]; their net calls share one [BatchingEvaluator].
 *
 * With [recordTo] (docs/65 §3, generation-2 data) each game is also written as `exit_selfplay.py`'s record,
 * `recordTo/g<game>.json.gz`: the deals line's own fields (game, kind, seats, decks), the outcome and tallies, and every
 * learner decision's wire observation, move, trace and search target, which `exit_fit.py` reads as it reads G1's.
 */
class Gate(
    private val engine: Engine,
    private val policy: GameplayPolicy,
    private val evaluator: Evaluator,
    private val arm: String,
    private val dealStream: Long,
    private val config: SearchConfig?,
    private val recordTo: File? = null,
    private val specs: Map<Int, JsonObject> = emptyMap(),
    private val checkpoint: String? = null,
) {
    private val trace = System.getProperty("e1.trace")?.let { File(it) }
    /** -De1.pyworlds=1: determinize with T1's world seeds (`world_seed(stream, deal, decision, w)`), as T1 did. */
    private val pythonWorlds = System.getProperty("e1.pyworlds") == "1"

    fun play(deal: Int, envConfig: EnvConfig, searcher: Searcher?): JsonObject {
        val t0 = System.nanoTime()
        val tally = sortedMapOf<String, Long>()
        fun add(key: String, n: Long = 1) { tally[key] = (tally[key] ?: 0) + n }
        val (env, opening) = engine.create(envConfig)
        var observation = opening.observation as TrainingObservation
        // The learner is the perspective seat; in a two-learner env (Track A's G0 opponent) the other seat is the
        // net sampled at T = 1, unsearched, and observations are the acting seat's.
        val learnerSeat = envConfig.perspectivePlayerIndex
        val player = env.environment.playerIds[learnerSeat].value
        val sampler = SplittableRandom(Searcher.decisionSeed(dealStream, deal.toLong(), -1))
        var searchNanos = 0L
        val decisions = if (recordTo != null) ArrayList<JsonObject>() else null
        while (true) {
            val status = env.status()
            if (status.terminated || status.truncated) break
            val mine = (status.actingSeat ?: learnerSeat) == learnerSeat
            var target: SearchTarget? = null
            val choice = try {
                when {
                    !mine -> {
                        val prepared = policy.prepare(observation)
                        val encoded = evaluator.encode(listOf(prepared)).single()
                        policy.decide(prepared, encoded, Picker.sample(sampler))
                    }
                    searcher != null -> {
                        val index = tally["decisions"] ?: 0
                        val (choice, stats) = searcher.decide(
                            env, observation, Searcher.decisionSeed(dealStream, deal.toLong(), index),
                            if (pythonWorlds) { w -> Blake2b.worldSeed(dealStream, deal.toLong(), index, w.toLong()) } else null,
                        )
                        if (stats.searched) {
                            add("searched")
                            searchNanos += stats.nanos
                        }
                        target = stats.target
                        stats.target?.let { t ->
                            // -De1.trace=FILE: every searched decision's candidates, visits and values, to check runs equal.
                            trace?.let { out ->
                                synchronized(out) {
                                    out.appendText(
                                        "$deal ${tally["decisions"] ?: 0} chosen=${t.chosen} " + t.candidates.joinToString(" ") {
                                            "${it.choice.key}:${it.visits}:${it.value?.let { v -> "%.6f".format(v) }}"
                                        } + "\n",
                                    )
                                }
                            }
                        }
                        add("simulations", stats.simulations.toLong())
                        if (stats.changedFromArgmax) add("changed")
                        add("worlds_skipped", stats.worldsSkipped.toLong())
                        add("dead_edges", stats.deadEdges.toLong())
                        add("leaf_fallbacks", stats.leafFallbacks.toLong())
                        add("root_edges", stats.rootEdges.toLong())
                        choice
                    }
                    else -> {
                        val prepared = policy.prepare(observation)
                        val encoded = evaluator.encode(listOf(prepared)).single()
                        policy.decide(prepared, encoded, if (arm == "A0") Picker.sample(sampler) else Picker.argmax())
                    }
                }
            } catch (_: InferenceError) {
                add(if (mine) "fallbacks" else "opponent_fallbacks")
                observation = env.playout(1).observation as TrainingObservation
                continue
            }
            add(if (mine) "decisions" else "opponent_decisions")
            if (mine) decisions?.add(decisionJson(observation, choice, target))
            observation = try {
                env.step(choice.actionId, choice.params.toActionParams()).observation as TrainingObservation
            } catch (e: Exception) {
                // The real env refused the move: count it and let the seat's pilot answer, as T1 did.
                add("refused:${e.javaClass.simpleName}:${e.message.orEmpty().take(60)}")
                // As exit_selfplay.py: a refused move's record is dropped.
                if (mine) decisions?.removeLastOrNull()
                env.playout(1).observation as TrainingObservation
            }
        }
        val status = env.status()
        val row = buildJsonObject {
            put("deal", deal)
            put("arm", arm)
            put("terminated", status.terminated)
            put("truncated", status.truncated)
            put("reward", status.reward.firstOrNull { it.playerId.value == player }?.value ?: 0.0)
            put("seconds", (System.nanoTime() - t0) / 1e9)
            put("search_seconds", searchNanos / 1e9)
            for ((k, v) in tally) put(k, v)
            put("engine", "e1")
        }
        if (decisions != null) writeRecord(deal, envConfig, row, decisions)
        return row
    }

    private fun writeRecord(deal: Int, envConfig: EnvConfig, row: JsonObject, decisions: List<JsonObject>) {
        val spec = specs[deal] ?: JsonObject(emptyMap())
        val game = spec["game"]?.jsonPrimitive?.int ?: deal
        val record = buildJsonObject {
            put("game", game)
            for ((k, v) in spec) if (k != "deal" && k != "game" && k != "config") put(k, v)
            for ((k, v) in row) if (k != "deal") put(k, v)
            // exit_selfplay.py's name for the learner's decision count, and the game's seed.
            put("learner_decisions", row["decisions"] ?: JsonPrimitive(0))
            put("seed", envConfig.seed?.let(::JsonPrimitive) ?: JsonNull)
            put("checkpoint", checkpoint)
            put("decisions", JsonArray(decisions))
        }
        val path = File(recordTo, "g%05d.json.gz".format(game))
        val tmp = File(recordTo, "g%05d.tmp".format(game))
        GZIPOutputStream(tmp.outputStream()).bufferedWriter().use {
            it.write(gymJson.encodeToString(JsonObject.serializer(), record))
        }
        Files.move(tmp.toPath(), path.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    /** Lane [lane] of [lanes]: deals lane, lane + lanes, ...; skips deals already in its file. */
    fun runLane(deals: List<Pair<Int, EnvConfig>>, out: File, lane: Int, lanes: Int, limit: Int) {
        val file = File(out, "$arm/lane$lane.jsonl").also { it.parentFile.mkdirs() }
        val done = if (file.exists()) file.readLines().filter { it.isNotBlank() }
            .map { gymJson.parseToJsonElement(it).jsonObject.getValue("deal").jsonPrimitive.int }.toSet() else emptySet()
        val mine = deals.filterIndexed { i, _ -> i % lanes == lane }.filter { it.first !in done }.take(limit)
        System.err.println("$arm lane $lane/$lanes: ${mine.size} deals to play (${done.size} done)")
        val searcher = config?.let { Searcher(policy, evaluator, it) }
        for ((deal, envConfig) in mine) {
            val row = play(deal, envConfig, searcher)
            file.appendText(gymJson.encodeToString(JsonObject.serializer(), row) + "\n")
            System.err.println(row)
        }
        searcher?.counts?.takeIf { it.isNotEmpty() }?.let { System.err.println("lane $lane counts $it") }
        searcher?.nanos?.let { n -> System.err.println("lane $lane ms " + n.mapValues { "%.0f".format(it.value / 1e6) }) }
    }

    companion object {
        private fun ints(values: IntArray): JsonElement = JsonArray(values.map { JsonPrimitive(it) })
        private fun params(p: Params): JsonElement = gymJson.parseToJsonElement(p.canonical())

        /** One learner decision as `exit_selfplay.py` records it; `obs` is the wire form, with its `"type"`. */
        fun decisionJson(observation: TrainingObservation, choice: Choice, target: SearchTarget?): JsonObject = buildJsonObject {
            put("obs", gymJson.encodeToJsonElement(Observation.serializer(), observation))
            put("action_id", choice.actionId)
            put("params", params(choice.params))
            put("trace", ints(choice.trace))
            put("target", target?.let(::targetJson) ?: JsonNull)
        }

        /** `SearchTarget.to_json`. */
        fun targetJson(t: SearchTarget): JsonObject = buildJsonObject {
            put("candidates", JsonArray(t.candidates.map { c ->
                buildJsonObject {
                    put("action_id", c.choice.actionId)
                    put("params", params(c.choice.params))
                    put("trace", ints(c.choice.trace))
                    put("logit", c.logit)
                    put("visits", c.visits)
                    put("value", c.value?.let(::JsonPrimitive) ?: JsonNull)
                }
            }))
            put("root_value", t.rootValue)
            put("chosen", t.chosen)
            put("noise", t.noise?.let { n -> JsonArray(n.map { JsonPrimitive(it) }) } ?: JsonNull)
        }

        /** Each deals line's own fields (everything but `config`), by deal: what a game record carries over. */
        fun readSpecs(file: File): Map<Int, JsonObject> = file.readLines().filter { it.isNotBlank() }.associate {
            val row = gymJson.parseToJsonElement(it).jsonObject
            row.getValue("deal").jsonPrimitive.int to JsonObject(row.filterKeys { k -> k != "config" })
        }

        fun readDeals(file: File): List<Pair<Int, EnvConfig>> = file.readLines().filter { it.isNotBlank() }.map {
            val row = gymJson.parseToJsonElement(it).jsonObject
            row.getValue("deal").jsonPrimitive.int to
                gymJson.decodeFromJsonElement(EnvConfig.serializer(), row.getValue("config"))
        }

        fun parseArm(arm: String): SearchConfig? = when {
            arm == "A0" || arm == "A0g" -> null
            // -De1.noise=1: docs/65 §2.2's root Gumbel noise, as self-play runs it.
            // -De1.rounds=0: each candidate's simulations back to back, unbatched (search.py's order; same result).
            arm.startsWith("S") -> SearchConfig(
                sims = arm.drop(1).toInt(),
                rootNoise = System.getProperty("e1.noise") == "1",
                batchRounds = System.getProperty("e1.rounds") != "0",
            )
            else -> error("unknown arm $arm")
        }
    }
}
