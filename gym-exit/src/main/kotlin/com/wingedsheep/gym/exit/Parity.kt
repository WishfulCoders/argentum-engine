package com.wingedsheep.gym.exit

import com.wingedsheep.gym.contract.TrainingObservation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.math.abs

/** The gym's wire format, as the Python side reads it (`WebConfig.gymJson`). */
val gymJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

/** `json.dumps(x, sort_keys=True, separators=(",", ":"))` for the ids-and-ints JSON the params carry. */
fun canonicalJson(e: JsonElement): String = when (e) {
    is JsonObject -> e.keys.sorted().joinToString(",", "{", "}") { "\"$it\":" + canonicalJson(e.getValue(it)) }
    is JsonArray -> e.joinToString(",", "[", "]") { canonicalJson(it) }
    is JsonNull -> "null"
    is JsonPrimitive -> if (e.isString) "\"${e.content}\"" else e.content
}

/**
 * docs/65 §3 items 2-3: hold the Kotlin featurizer, the ONNX encoder and the ported heads to the Python policy on
 * fixtures from `scripts/e1_record.py`. Features and every discrete output must be equal; log-probabilities and
 * values within [TOLERANCE]. Rows are encoded in padded batches of [batchSize], so batching is under test too.
 */
class Parity(private val policy: GameplayPolicy, private val batchSize: Int = 16) {
    private val logitDiffs = ArrayList<Double>()
    private val relativeDiffs = ArrayList<Double>()
    private val counts = sortedMapOf<String, Int>()
    private val worst = sortedMapOf<String, Double>()
    private val failures = ArrayList<String>()

    private fun count(key: String, n: Int = 1) { counts[key] = (counts[key] ?: 0) + n }
    private fun diff(key: String, value: Double) { if (value > (worst[key] ?: 0.0)) worst[key] = value }
    private fun fail(row: Int, what: String) {
        count("fail:${what.substringBefore(':')}")
        if (failures.size < 400) failures += "row $row: $what"
    }

    fun run(files: List<File>, limit: Int): JsonObject {
        val pending = ArrayList<Triple<Int, JsonObject, Prepared>>()
        var row = 0
        val t0 = System.nanoTime()
        var featureNanos = 0L
        outer@ for (file in files) {
            GZIPInputStream(file.inputStream()).bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (row >= limit) return@useLines
                    val json = gymJson.parseToJsonElement(line).jsonObject
                    val obs = gymJson.decodeFromJsonElement(TrainingObservation.serializer(), json.getValue("obs"))
                    count("rows")
                    val f0 = System.nanoTime()
                    val prepared = try {
                        policy.prepare(obs)
                    } catch (e: InferenceError) {
                        if ("error" in json) count("both_refused") else fail(row, "refused: ${e.message}")
                        row++
                        continue
                    }
                    featureNanos += System.nanoTime() - f0
                    if ("error" in json) {
                        fail(row, "python refused: ${json["error"]}")
                        row++
                        continue
                    }
                    checkFeatures(row, json.getValue("features").jsonObject, prepared)
                    pending += Triple(row, json, prepared)
                    if (pending.size == batchSize) flush(pending)
                    row++
                }
            }
        }
        if (pending.isNotEmpty()) flush(pending)
        return kotlinx.serialization.json.buildJsonObject {
            put("counts", JsonObject(counts.mapValues { JsonPrimitive(it.value) }))
            put("max_abs_diff", JsonObject(worst.mapValues { JsonPrimitive(it.value) }))
            fun pct(xs: List<Double>, q: Double) = xs.sorted().let { if (it.isEmpty()) 0.0 else it[minOf(it.size - 1, (q * it.size).toInt())] }
            put("action_logit_abs_p50_p99_p999", JsonArray(listOf(0.5, 0.99, 0.999).map { JsonPrimitive(pct(logitDiffs, it)) }))
            put("action_logit_rel_p50_p99_p999", JsonArray(listOf(0.5, 0.99, 0.999).map { JsonPrimitive(pct(relativeDiffs, it)) }))
            put("action_logit_abs_over_1e-5", JsonPrimitive(logitDiffs.count { it > 1e-5 }))
            put("action_logits_compared", JsonPrimitive(logitDiffs.size))
            put("featurize_ms_mean", JsonPrimitive(featureNanos / 1e6 / maxOf(1, counts["rows"] ?: 1)))
            put("seconds", JsonPrimitive((System.nanoTime() - t0) / 1e9))
            put("failures", JsonArray(failures.map { JsonPrimitive(it) }))
        }
    }

    private fun ints(e: JsonElement): List<Int> = e.jsonArray.map { it.jsonPrimitive.int }
    private fun matrix(e: JsonElement): List<List<Int>> = e.jsonArray.map { ints(it) }

    private fun checkFeatures(row: Int, want: JsonObject, p: Prepared) {
        val f = p.features
        val checks = listOf(
            "glob" to (ints(want.getValue("glob")) == f.glob.toList()),
            "players" to (matrix(want.getValue("players")) == f.players.map { it.toList() }),
            "entities" to (matrix(want.getValue("entities")) == f.entities.map { it.toList() }),
            "cards" to (want.getValue("cards").jsonArray.map { it.jsonPrimitive.content } == f.cards),
            "card_ids" to (ints(want.getValue("card_ids")) == f.cards.map { policy.net.cardId(it) }),
            "actions" to (matrix(want.getValue("actions")) == f.actions.map { it.toList() }),
            "action_source" to (ints(want.getValue("action_source")) == f.actionSource.toList()),
            "action_description" to (ints(want.getValue("action_description")) == f.actionDescription.toList()),
            "action_ids" to (ints(want.getValue("action_ids")) == f.actionIds.toList()),
            "pointer_ids" to (want.getValue("pointer_ids").jsonArray.map { it.jsonPrimitive.content } == f.pointerIds),
            "supported" to (want.getValue("supported").jsonArray.map { it.jsonPrimitive.booleanOrNull } == p.supported.toList()),
        )
        var all = true
        for ((name, ok) in checks) if (!ok) { fail(row, "features:$name"); all = false }
        if (all) count("features_equal")
    }

    private fun flush(pending: MutableList<Triple<Int, JsonObject, Prepared>>) {
        val encoded = policy.encode(pending.map { it.third })
        for ((i, item) in pending.withIndex()) {
            val (row, json, p) = item
            try {
                checkOutputs(row, json, p, encoded[i])
            } catch (e: Exception) {
                fail(row, "exception: ${e.javaClass.simpleName} ${e.message}")
            }
        }
        pending.clear()
    }

    private fun sameChoice(row: Int, what: String, want: JsonObject, got: Choice) {
        val params = canonicalJson(want.getValue("params"))
        val trace = ints(want.getValue("trace"))
        if (want.getValue("action_id").jsonPrimitive.int != got.actionId) fail(row, "$what:action ${want["action_id"]} vs ${got.actionId}")
        else if (trace != got.trace.toList()) fail(row, "$what:trace $trace vs ${got.trace.toList()}")
        else if (params != got.params.canonical()) fail(row, "$what:params $params vs ${got.params.canonical()}")
        else count("${what}_equal")
        diff("${what}_log_probability", abs(want.getValue("log_probability").jsonPrimitive.double - got.logProbability))
    }

    private fun checkOutputs(row: Int, json: JsonObject, p: Prepared, enc: Encoded) {
        diff("value", abs(json.getValue("value").jsonPrimitive.double - enc.value))
        val mask = json.getValue("mask").jsonArray.map { it.jsonPrimitive.booleanOrNull }
        if (mask != p.mask.toList()) fail(row, "mask")
        val want = json.getValue("logits").jsonArray.map { it.jsonPrimitive.double }
        val got = policy.actionLogits(p, enc)
        for (i in got.indices) if (p.mask[i]) {
            val d = abs(want[i] - got[i])
            diff("action_logit", d)
            logitDiffs += d
            relativeDiffs += d / maxOf(1.0, abs(want[i]))
            diff("action_logit_relative", d / maxOf(1.0, abs(want[i])))
        }
        // Where the two orderings could disagree: Python's gap between its top two allowed logits.
        val ranked = got.indices.filter { p.mask[it] }.sortedByDescending { want[it] }
        if (ranked.size >= 2) {
            val gap = want[ranked[0]] - want[ranked[1]]
            if (gap < 1e-4) count("python_top2_gap_below_1e-4")
            if (gap == 0.0) count("python_top2_exact_tie")
            val mine = got.indices.filter { p.mask[it] }.maxWith(compareBy<Int> { got[it] }.thenByDescending { it })
            if (mine != ranked.first { want[it] == want[ranked[0]] }) {
                count("argmax_swap")
                diff("argmax_swap_python_gap", gap)
            }
        }

        val top = json["top"]?.jsonArray
        if (top != null) {
            val mine = try { policy.topActions(p, enc, 8) } catch (e: InferenceError) { emptyList() }
            if (mine.size != top.size) {
                fail(row, "top:size ${top.size} vs ${mine.size}")
            } else {
                for ((j, w) in top.withIndex()) {
                    val wantId = w.jsonObject.getValue("action_id").jsonPrimitive.int
                    if (wantId != mine[j].first.actionId) {
                        val a = p.features.actionIds.indexOf(wantId)
                        val b = mine[j].first.actionIndex
                        diff("top_swap_python_gap", abs(want[a] - want[b]))
                    }
                    sameChoice(row, "top", w.jsonObject, mine[j].first)
                    diff("top_head_log_p", abs(w.jsonObject.getValue("head_log_p").jsonPrimitive.double - mine[j].second))
                }
                count("top_lists")
            }
        } else if (json["top_error"] != null) count("top_error")
        val argmax = json.getValue("argmax").jsonObject
        if ("error" in argmax) {
            try {
                policy.decide(p, enc, Picker.argmax())
                fail(row, "argmax:python refused ${argmax["error"]}")
            } catch (_: InferenceError) { count("argmax_both_refused") }
        } else {
            try { sameChoice(row, "argmax", argmax, policy.decide(p, enc, Picker.argmax())) }
            catch (e: InferenceError) { fail(row, "argmax:refused ${e.message}") }
        }
        for (s in json.getValue("samples").jsonArray) {
            val sample = s.jsonObject
            if ("error" in sample) { count("sample_python_error"); continue }
            val trace = ints(sample.getValue("trace")).toIntArray()
            if (trace.size > 1) count("multi_step_samples")
            try { sameChoice(row, "sample", sample, policy.replay(p, enc, trace)) }
            catch (e: InferenceError) { fail(row, "sample:refused ${e.message} trace ${trace.toList()}") }
        }
    }
}
