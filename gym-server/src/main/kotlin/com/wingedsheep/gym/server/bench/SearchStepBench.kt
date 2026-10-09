package com.wingedsheep.gym.server.bench

import com.wingedsheep.gym.GameGymEnv
import com.wingedsheep.gym.contract.LegalActionView
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.gym.server.config.GymBeansConfig
import com.wingedsheep.gym.service.EnvConfig
import com.wingedsheep.gym.service.MultiEnvService
import com.wingedsheep.gym.service.StepRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import kotlin.random.Random

/**
 * mtg-draft-ai docs/64 §2 M2: what one search simulation costs **in-process**, through the same [MultiEnvService]
 * the HTTP gym wraps, so the difference from M1 (`scripts/search_cost_bench.py`) is the transport alone.
 *
 * Input: JSONL of `{"mode": "pilot"|"selfplay", "warmup": bool, "config": EnvConfig}` (written by
 * `search_cost_bench.py --dump-configs`). Each game is driven by the learner seat's own pilot
 * (`playout(maxLearnerActions = 1)`). At every third decision with two or more legal actions, up to
 * `positionsPerGame` times, one simulation is timed on a fork:
 *
 *     fork -> determinize -> snapshot -> observe -> [json encode] -> step -> restore
 *
 * The stepped action is drawn from the parameter-free legal actions (pass half the time, else uniform over the
 * rest), because a cost measurement needs a legal move, not a good one. A refused step is counted and skipped.
 * Warmup games (JIT) are timed under their own key and left out of the modes.
 *
 * Run: `java -Dloader.main=com.wingedsheep.gym.server.bench.SearchStepBenchKt -cp gym-server.jar
 *       org.springframework.boot.loader.launch.PropertiesLauncher CONFIGS OUT [positionsPerGame]`
 */
fun main(args: Array<String>) {
    require(args.size >= 2) { "usage: SearchStepBench CONFIGS.jsonl OUT.json [positionsPerGame]" }
    val positionsPerGame = args.getOrNull(2)?.toInt() ?: 10
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val beans = GymBeansConfig()
    val service = MultiEnvService(beans.cardRegistry(), beans.boosterGenerator())
    val rng = Random(64)
    val bench = mutableMapOf<String, Stats>()
    val wall0 = System.nanoTime()

    for ((index, line) in File(args[0]).readLines().filter { it.isNotBlank() }.withIndex()) {
        val root = json.parseToJsonElement(line).jsonObject
        val warm = root["warmup"]?.jsonPrimitive?.boolean ?: false
        val key = if (warm) "warmup" else root["mode"]!!.jsonPrimitive.content
        val stats = bench.getOrPut(key) { Stats() }
        val config = json.decodeFromJsonElement(EnvConfig.serializer(), root["config"]!!)
        val created = service.create(config)
        val env = created.envId
        var observation = created.observation.observation
        var measured = 0
        var eligible = 0
        while (true) {
            val status = service.status(env)
            if (status.terminated || status.truncated) break
            val legal = observation.legalActions
            if (legal.size >= 2) eligible++
            if (legal.size >= 2 && measured < positionsPerGame && eligible % 3 == 0) {
                measured++
                val fork = stats.time("fork") { service.fork(env).single() }
                stats.time("determinize") { service.determinize(fork, rng.nextLong()) }
                val handle = stats.time("snapshot") { service.snapshot(fork) }
                val world = stats.time("observe") { service.observe(fork) }
                val obs = world.observation as TrainingObservation
                val bytes = stats.time("json_encode") {
                    json.encodeToString(TrainingObservation.serializer(), obs).length
                }
                stats.add("observation_bytes", bytes.toDouble())
                val action = pick(obs.legalActions, rng)
                if (action == null) {
                    stats.count("no_param_free_action")
                } else {
                    val before = service.status(fork).stepCount
                    try {
                        val stepT0 = System.nanoTime()
                        service.step(StepRequest(fork, action.actionId))
                        val stepMs = (System.nanoTime() - stepT0) / 1e6
                        stats.add("step", stepMs)
                        stats.add("engine_steps", (service.status(fork).stepCount - before).toDouble())
                        // `step` ends by rebuilding the observation (legal actions included). Building it again on
                        // the same state prices that part, so step - observe_after_step is the rules work alone:
                        // the step an in-JVM search pays when it skips per-node observations (docs/64 §4.4).
                        val buildT0 = System.nanoTime()
                        service.observe(fork)
                        val buildMs = (System.nanoTime() - buildT0) / 1e6
                        stats.add("observe_after_step", buildMs)
                        stats.add("step_raw", stepMs - buildMs)
                        // The legal moves alone (the part of the observation a search node does need), on the same
                        // post-step state.
                        val legalEnv = gameEnv(service, fork).environment
                        stats.time("legal_actions") { legalEnv.legalActions() }
                        stats.time("restore") { service.restore(fork, handle) }
                    } catch (e: Exception) {
                        stats.count("refused:${e.javaClass.simpleName}")
                    }
                }
                service.snapshotCodec.dispose(handle)
                service.dispose(listOf(fork))
            }
            observation = service.playout(env, 1).observation
            stats.count("decisions")
        }
        service.dispose(listOf(env))
        stats.count("games")
        System.err.println("game ${index + 1} [$key]: ${stats.samples["step"]?.size ?: 0} steps timed")
    }

    val out = buildJsonObject {
        put("wall_s", (System.nanoTime() - wall0) / 1e9)
        put("positions_per_game", positionsPerGame)
        put("jvm_processors", Runtime.getRuntime().availableProcessors())
        for ((key, stats) in bench) put(key, stats.summary())
    }
    File(args[1]).writeText(json.encodeToString(JsonObject.serializer(), out))
    println(out)
}

private val envsField = MultiEnvService::class.java.getDeclaredField("envs").apply { isAccessible = true }

/** The service keeps its envs private; the bench reads one through reflection rather than widen the service API. */
private fun gameEnv(service: MultiEnvService, id: com.wingedsheep.gym.service.EnvId): GameGymEnv =
    (envsField.get(service) as Map<*, *>)[id] as GameGymEnv

/** A parameter-free legal move: pass half the time, else uniform over the rest. */
private fun pick(legal: List<LegalActionView>, rng: Random): LegalActionView? {
    val free = legal.filter {
        it.affordable && !it.isManaAbility && !it.hasXCost && !it.requiresDamageDistribution &&
            // minTargets reads 1 even on a pass, so "needs a target" is read from the target lists themselves.
            it.targetEntityIds.isEmpty() && it.targetRequirements.isEmpty() && !it.kind.startsWith("Declare")
    }
    if (free.isEmpty()) return null
    val pass = free.firstOrNull { it.kind == "PassPriority" }
    val rest = free.filter { it.kind != "PassPriority" }
    return if (pass != null && (rest.isEmpty() || rng.nextBoolean())) pass else rest.random(rng)
}

private class Stats {
    val samples = mutableMapOf<String, MutableList<Double>>()
    val counts = mutableMapOf<String, Int>()

    inline fun <T> time(key: String, block: () -> T): T {
        val t0 = System.nanoTime()
        val result = block()
        add(key, (System.nanoTime() - t0) / 1e6)
        return result
    }

    fun add(key: String, value: Double) {
        samples.getOrPut(key) { mutableListOf() }.add(value)
    }

    fun count(key: String) {
        counts[key] = (counts[key] ?: 0) + 1
    }

    fun summary(): JsonObject = buildJsonObject {
        for ((key, values) in samples) {
            val sorted = values.sorted()
            put(key, buildJsonObject {
                put("n", sorted.size)
                put("mean", sorted.average())
                put("p50", sorted[sorted.size / 2])
                put("p95", sorted[minOf(sorted.size - 1, (0.95 * sorted.size).toInt())])
            })
        }
        // One simulation on a node env, in-process: restore + step + observe (no JSON), and with JSON added.
        val sim = listOf("restore", "step", "observe").mapNotNull { samples[it] }
        if (sim.size == 3) {
            val n = sim.minOf { it.size }
            val perSim = (0 until n).map { i -> sim.sumOf { it[i] } }.sorted()
            put("per_simulation_ms", buildJsonObject {
                put("n", n); put("p50", perSim[n / 2]); put("mean", perSim.average())
                put("p95", perSim[minOf(n - 1, (0.95 * n).toInt())])
            })
        }
        put("counts", buildJsonObject { for ((k, v) in counts) put(k, v) })
    }
}
