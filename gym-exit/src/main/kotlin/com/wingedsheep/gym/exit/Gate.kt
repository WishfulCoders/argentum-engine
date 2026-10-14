package com.wingedsheep.gym.exit

import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.gym.service.EnvConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.util.SplittableRandom

/**
 * docs/65 §3 item 4: `scripts/search_t1.py`'s game loop, in the JVM. Plays T1's deals (the env configs
 * `scripts/e1_deals.py` writes from `deals.jsonl`) with one arm and writes one row per game in T1's format to
 * `OUT/<arm>/lane<i>.jsonl`, resumable, so the same deals can be compared deal by deal.
 *
 * Arms: `A0g` (argmax of every head), `A0` (sampled at T = 1) and `S<n>` (the search at n simulations). Lanes are
 * threads in this JVM, each with its own envs and [Searcher]; their net calls share one [BatchingEvaluator].
 */
class Gate(
    private val engine: Engine,
    private val policy: GameplayPolicy,
    private val evaluator: Evaluator,
    private val arm: String,
    private val dealStream: Long,
    private val config: SearchConfig?,
) {
    fun play(deal: Int, envConfig: EnvConfig, searcher: Searcher?): JsonObject {
        val t0 = System.nanoTime()
        val tally = sortedMapOf<String, Long>()
        fun add(key: String, n: Long = 1) { tally[key] = (tally[key] ?: 0) + n }
        val (env, opening) = engine.create(envConfig)
        var observation = opening.observation as TrainingObservation
        val player = observation.perspectivePlayerId.value
        val sampler = SplittableRandom(Searcher.decisionSeed(dealStream, deal.toLong(), -1))
        var searchNanos = 0L
        while (true) {
            val status = env.status()
            if (status.terminated || status.truncated) break
            val choice = try {
                when {
                    searcher != null -> {
                        val (choice, stats) = searcher.decide(
                            env, observation, Searcher.decisionSeed(dealStream, deal.toLong(), tally["decisions"] ?: 0),
                        )
                        if (stats.searched) {
                            add("searched")
                            searchNanos += stats.nanos
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
                add("fallbacks")
                observation = env.playout(1).observation as TrainingObservation
                continue
            }
            add("decisions")
            observation = try {
                env.step(choice.actionId, choice.params.toActionParams()).observation as TrainingObservation
            } catch (e: Exception) {
                // The real env refused the move: count it and let the seat's pilot answer, as T1 did.
                add("refused:${e.javaClass.simpleName}:${e.message.orEmpty().take(60)}")
                env.playout(1).observation as TrainingObservation
            }
        }
        val status = env.status()
        return buildJsonObject {
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
        fun readDeals(file: File): List<Pair<Int, EnvConfig>> = file.readLines().filter { it.isNotBlank() }.map {
            val row = gymJson.parseToJsonElement(it).jsonObject
            row.getValue("deal").jsonPrimitive.int to
                gymJson.decodeFromJsonElement(EnvConfig.serializer(), row.getValue("config"))
        }

        fun parseArm(arm: String): SearchConfig? = when {
            arm == "A0" || arm == "A0g" -> null
            arm.startsWith("S") -> SearchConfig(sims = arm.drop(1).toInt())
            else -> error("unknown arm $arm")
        }
    }
}
