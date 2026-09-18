package com.wingedsheep.gym.server

import com.wingedsheep.gym.contract.Observation
import com.wingedsheep.gym.service.DeckSpec
import com.wingedsheep.gym.service.EnvConfig
import com.wingedsheep.gym.service.EnvId
import com.wingedsheep.gym.service.MultiEnvService
import com.wingedsheep.gym.service.PlayerSpec
import com.wingedsheep.gym.service.StepRequest
import com.wingedsheep.gym.server.dto.CreateEnvResponse
import com.wingedsheep.gym.server.dto.DisposeBody
import com.wingedsheep.gym.server.dto.StepBatchItem
import com.wingedsheep.gym.server.dto.StepBatchResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Locale
import kotlin.system.measureNanoTime

/**
 * Measures the decision-loop boundary needed to choose between a Python-owned HTTP gym and a
 * JVM-owned rollout loop. Both arms call the same [MultiEnvService.stepBatch] implementation and
 * submit only PassPriority, so the delta is transport + JSON rather than policy or card behavior.
 *
 * Disabled by default. Run with:
 *
 * ```
 * just benchmark-gym-transport
 * # or:
 * scripts/gradle-locked :gym-server:test --tests "*.GymTransportBenchmark" \
 *   -Dbenchmark=true -DbenchmarkEnvs=16 -DbenchmarkRounds=100 -DbenchmarkForks=1000
 * ```
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GymTransportBenchmark : FunSpec() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var service: MultiEnvService

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
    private val client = HttpClient.newBuilder().build()

    init {
        extension(SpringExtension())

        val enabled = System.getProperty("benchmark") == "true"
        val envCount = System.getProperty("benchmarkEnvs")?.toIntOrNull() ?: 16
        val rounds = System.getProperty("benchmarkRounds")?.toIntOrNull() ?: 100
        val warmupRounds = System.getProperty("benchmarkWarmupRounds")?.toIntOrNull() ?: 10
        val forkCount = System.getProperty("benchmarkForks")?.toIntOrNull() ?: 1_000

        test("benchmark: in-process versus HTTP gym transport").config(enabled = enabled) {
            require(envCount > 0 && rounds > 0 && warmupRounds >= 0 && forkCount > 0)
            val batchSizes = listOf(1, envCount).distinct()
            val rows = mutableListOf<StepMeasurement>()

            for (batchSize in batchSizes) {
                rows += driveInProcess(envCount, batchSize, warmupRounds, rounds)
                rows += driveHttp(envCount, batchSize, warmupRounds, rounds)
            }

            println("=== GYM TRANSPORT BENCHMARK ===")
            println(
                "envs=$envCount rounds=$rounds warmupRounds=$warmupRounds " +
                    "decisions/path=${envCount * rounds}"
            )
            println(
                String.format(
                    Locale.US,
                    "%-12s %6s %10s %12s %14s %15s",
                    "path", "batch", "decisions", "seconds", "decisions/s", "wire B/decision"
                )
            )
            rows.forEach { row ->
                println(
                    String.format(
                        Locale.US,
                        "%-12s %6d %10d %12.3f %14.1f %15.1f",
                        row.path,
                        row.batchSize,
                        row.decisions,
                        row.nanos / 1e9,
                        row.decisions * 1e9 / row.nanos,
                        row.responseBytes.toDouble() / row.decisions
                    )
                )
            }

            val directFork = benchmarkDirectFork(forkCount)
            val httpFork = benchmarkHttpFork(forkCount)
            println()
            println(String.format(Locale.US, "%-12s %10s %12s %14s", "fork path", "children", "total ms", "us/child"))
            for (row in listOf(directFork, httpFork)) {
                println(
                    String.format(
                        Locale.US,
                        "%-12s %10d %12.3f %14.3f",
                        row.path,
                        row.children,
                        row.nanos / 1e6,
                        row.nanos / 1e3 / row.children
                    )
                )
            }
        }
    }

    private fun config(): EnvConfig {
        val deck = DeckSpec.Explicit(mapOf("Mountain" to 40))
        return EnvConfig(
            players = listOf(PlayerSpec("Alice", deck), PlayerSpec("Bob", deck)),
            skipMulligans = true,
            startingPlayerIndex = 0
        )
    }

    private fun actionId(observation: Observation): Int =
        observation.legalActions.firstOrNull {
            it.kind.contains("pass", ignoreCase = true) ||
                it.description.contains("pass", ignoreCase = true)
        }?.actionId
            ?: observation.legalActions.firstOrNull { it.affordable }?.actionId
            ?: error(
                "benchmark reached a state without an affordable action: " +
                    observation.legalActions.joinToString { "${it.kind}:${it.description}" }
            )

    private fun driveInProcess(
        envCount: Int,
        batchSize: Int,
        warmupRounds: Int,
        rounds: Int
    ): StepMeasurement {
        val created = List(envCount) { service.create(config()) }
        val observations = created.associate { it.envId to it.observation.observation }.toMutableMap()

        fun drive(measured: Boolean): Pair<Long, Int> {
            var elapsed = 0L
            var decisions = 0
            for (ids in created.map { it.envId }.chunked(batchSize)) {
                for (id in ids) {
                    if (
                        observations.getValue(id).terminated ||
                        observations.getValue(id).legalActions.isEmpty()
                    ) {
                        observations[id] = service.reset(id, config()).observation
                    }
                }
                val requests = ids.map { id -> StepRequest(id, actionId(observations.getValue(id))) }
                val result: List<Pair<EnvId, com.wingedsheep.gym.contract.ObservationResult>>
                val nanos = measureNanoTime { result = service.stepBatch(requests) }
                if (measured) elapsed += nanos
                for ((id, next) in result) observations[id] = next.observation
                decisions += result.size
            }
            return elapsed to decisions
        }

        repeat(warmupRounds) { drive(false) }
        var nanos = 0L
        var decisions = 0
        repeat(rounds) {
            val row = drive(true)
            nanos += row.first
            decisions += row.second
        }
        service.dispose(created.map { it.envId })
        return StepMeasurement("in-process", batchSize, decisions, nanos, 0)
    }

    private fun driveHttp(
        envCount: Int,
        batchSize: Int,
        warmupRounds: Int,
        rounds: Int
    ): StepMeasurement {
        val created = List(envCount) {
            decode<CreateEnvResponse>(post("/envs", json.encodeToString(config())).body())
        }
        val observations = created.associate { it.envId to it.observation }.toMutableMap()

        fun drive(measured: Boolean): Triple<Long, Int, Long> {
            var elapsed = 0L
            var decisions = 0
            var bytes = 0L
            for (ids in created.map { it.envId }.chunked(batchSize)) {
                for (id in ids) {
                    if (
                        observations.getValue(id).terminated ||
                        observations.getValue(id).legalActions.isEmpty()
                    ) {
                        observations[id] = decode(
                            post("/envs/${id.value}/reset", json.encodeToString(config())).body()
                        )
                    }
                }
                val items = ids.map { id -> StepBatchItem(id, actionId(observations.getValue(id))) }
                lateinit var response: HttpResponse<String>
                lateinit var result: List<StepBatchResult>
                val nanos = measureNanoTime {
                    response = post("/envs/step-batch", json.encodeToString(items))
                    result = decode(response.body())
                }
                if (measured) {
                    elapsed += nanos
                    bytes += response.body().toByteArray().size
                }
                for (next in result) observations[next.envId] = next.observation
                decisions += result.size
            }
            return Triple(elapsed, decisions, bytes)
        }

        repeat(warmupRounds) { drive(false) }
        var nanos = 0L
        var decisions = 0
        var bytes = 0L
        repeat(rounds) {
            val row = drive(true)
            nanos += row.first
            decisions += row.second
            bytes += row.third
        }
        delete("/envs", json.encodeToString(DisposeBody(created.map { it.envId })))
        return StepMeasurement("HTTP", batchSize, decisions, nanos, bytes)
    }

    private fun benchmarkDirectFork(count: Int): ForkMeasurement {
        val source = service.create(config()).envId
        lateinit var children: List<EnvId>
        val nanos = measureNanoTime { children = service.fork(source, count) }
        service.dispose(children + source)
        return ForkMeasurement("in-process", count, nanos)
    }

    private fun benchmarkHttpFork(count: Int): ForkMeasurement {
        val source = decode<CreateEnvResponse>(post("/envs", json.encodeToString(config())).body()).envId
        lateinit var response: HttpResponse<String>
        lateinit var children: List<EnvId>
        val nanos = measureNanoTime {
            response = post("/envs/${source.value}/fork?count=$count", "")
            children = decode(response.body())
        }
        delete("/envs", json.encodeToString(DisposeBody(children + source)))
        return ForkMeasurement("HTTP", count, nanos)
    }

    private inline fun <reified T> decode(body: String): T = json.decodeFromString(body)

    private fun post(path: String, body: String): HttpResponse<String> {
        val response = client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        check(response.statusCode() == 200) {
            "POST $path returned ${response.statusCode()}: ${response.body()}"
        }
        return response
    }

    private fun delete(path: String, body: String) {
        val response = client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        check(response.statusCode() == 204) {
            "DELETE $path returned ${response.statusCode()}: ${response.body()}"
        }
    }
}

private data class StepMeasurement(
    val path: String,
    val batchSize: Int,
    val decisions: Int,
    val nanos: Long,
    val responseBytes: Long
)

private data class ForkMeasurement(
    val path: String,
    val children: Int,
    val nanos: Long
)
