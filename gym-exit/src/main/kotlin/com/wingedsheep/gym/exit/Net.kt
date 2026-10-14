package com.wingedsheep.gym.exit

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/** One observation's encoder outputs, in that observation's own pointer layout (entities, then the two players). */
class Encoded(
    val glob: FloatArray,
    val pointer: Array<FloatArray>,
    val actionFeatures: Array<FloatArray>,
    val actionScores: FloatArray,
    val value: Float,
)

/**
 * The S1 policy net for the in-JVM search (docs/65 §3 item 3): the encoder (plus the action head and critic) runs in
 * ONNX Runtime from `scripts/e1_export.py`'s `encoder.onnx`; the pick-at-a-time heads (X, target pointer, combat) run
 * here from `heads.bin`, in double precision over float32 weights, mirroring `PolicyNet`.
 */
class PolicyNet(dir: File, intraOpThreads: Int = 1, optLevel: String = "all", modelFile: String = "encoder.onnx") : AutoCloseable {
    val vocabulary: Map<String, Int>
    val width: Int
    val xOut: Int
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val weights: Map<String, FloatArray>
    private val shapes: Map<String, List<Int>>

    init {
        val json = Json { ignoreUnknownKeys = true }
        vocabulary = json.parseToJsonElement(File(dir, "vocab.json").readText()).jsonObject
            .mapValues { it.value.jsonPrimitive.int }
        val config = json.parseToJsonElement(File(dir, "config.json").readText()).jsonObject
        width = config.getValue("width").jsonPrimitive.int
        xOut = config.getValue("x_out").jsonPrimitive.int
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(intraOpThreads)
            setInterOpNumThreads(1)
            setOptimizationLevel(
                when (optLevel) {
                    "none" -> OrtSession.SessionOptions.OptLevel.NO_OPT
                    "basic" -> OrtSession.SessionOptions.OptLevel.BASIC_OPT
                    "extended" -> OrtSession.SessionOptions.OptLevel.EXTENDED_OPT
                    else -> OrtSession.SessionOptions.OptLevel.ALL_OPT
                },
            )
        }
        session = env.createSession(File(dir, modelFile).path, options)
        val raw = ByteBuffer.wrap(File(dir, "heads.bin").readBytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val index = json.parseToJsonElement(File(dir, "heads.json").readText()).jsonObject
        val w = HashMap<String, FloatArray>()
        val s = HashMap<String, List<Int>>()
        for ((name, entry) in index) {
            val shape = entry.jsonObject.getValue("shape").jsonArray.map { it.jsonPrimitive.int }
            val offset = entry.jsonObject.getValue("offset").jsonPrimitive.int
            val size = shape.fold(1) { a, b -> a * b }
            val out = FloatArray(size)
            raw.position(offset)
            raw.get(out)
            w[name] = out
            s[name] = shape
        }
        weights = w
        shapes = s
    }

    fun cardId(name: String): Int = vocabulary[name] ?: if (name.isEmpty()) 0 else 1

    // -- the encoder ------------------------------------------------------------------------------------------

    /** One padded encoder pass over [batch]; each row comes back in its own layout, as `inference._row` makes it. */
    fun encode(batch: List<Features>): List<Encoded> {
        val b = batch.size
        val e = maxOf(1, batch.maxOf { it.entityCount })
        val a = batch.maxOf { it.actionCount }
        require(a > 0) { "an observation with no actions" }
        val gw = FeatureSpec.GLOBAL_WIDTH
        val pw = FeatureSpec.PLAYER_WIDTH
        val ew = FeatureSpec.ENTITY_WIDTH
        val aw = FeatureSpec.ACTION_WIDTH
        val glob = FloatBuffer.allocate(b * gw)
        val players = FloatBuffer.allocate(b * 2 * pw)
        val entities = FloatBuffer.allocate(b * e * ew)
        val cards = LongBuffer.allocate(b * e)
        val entityMask = ByteBuffer.allocateDirect(b * e)
        val actions = FloatBuffer.allocate(b * a * aw)
        val source = LongBuffer.allocate(b * a)
        val description = LongBuffer.allocate(b * a)
        val actionMask = ByteBuffer.allocateDirect(b * a)
        for ((row, f) in batch.withIndex()) {
            for (v in f.glob) glob.put(v.toFloat())
            for (pl in f.players) for (v in pl) players.put(v.toFloat())
            for (i in 0 until e) {
                if (i < f.entityCount) {
                    for (v in f.entities[i]) entities.put(v.toFloat())
                    cards.put(cardId(f.cards[i]).toLong())
                    entityMask.put(1)
                } else {
                    entities.position(entities.position() + ew)
                    cards.put(0)
                    entityMask.put(0)
                }
            }
            for (i in 0 until a) {
                if (i < f.actionCount) {
                    for (v in f.actions[i]) actions.put(v.toFloat())
                    source.put(f.actionSource[i].toLong())
                    description.put(f.actionDescription[i].toLong())
                    actionMask.put(if (f.actions[i][FeatureSpec.AFFORDABLE_COLUMN] != 0) 1 else 0)
                } else {
                    actions.position(actions.position() + aw)
                    source.put(-1)
                    description.put(0)
                    actionMask.put(0)
                }
            }
            check(row + 1 == glob.position() / gw)
        }
        for (buffer in listOf(glob, players, entities, actions)) buffer.rewind()
        for (buffer in listOf(cards, source, description)) buffer.rewind()
        entityMask.rewind()
        actionMask.rewind()
        val b64 = b.toLong()
        val inputs = linkedMapOf(
            "glob" to OnnxTensor.createTensor(env, glob, longArrayOf(b64, gw.toLong())),
            "players" to OnnxTensor.createTensor(env, players, longArrayOf(b64, 2, pw.toLong())),
            "entities" to OnnxTensor.createTensor(env, entities, longArrayOf(b64, e.toLong(), ew.toLong())),
            "cards" to OnnxTensor.createTensor(env, cards, longArrayOf(b64, e.toLong())),
            "entity_mask" to OnnxTensor.createTensor(env, entityMask, longArrayOf(b64, e.toLong()), OnnxJavaType.BOOL),
            "actions" to OnnxTensor.createTensor(env, actions, longArrayOf(b64, a.toLong(), aw.toLong())),
            "action_source" to OnnxTensor.createTensor(env, source, longArrayOf(b64, a.toLong())),
            "action_description" to OnnxTensor.createTensor(env, description, longArrayOf(b64, a.toLong())),
            "action_mask" to OnnxTensor.createTensor(env, actionMask, longArrayOf(b64, a.toLong()), OnnxJavaType.BOOL),
        )
        try {
            session.run(inputs).use { result ->
                val d = width
                val globOut = (result.get("glob_out").get() as OnnxTensor).floatBuffer
                val pointer = (result.get("pointer").get() as OnnxTensor).floatBuffer
                val features = (result.get("action_features").get() as OnnxTensor).floatBuffer
                val scores = (result.get("action_scores").get() as OnnxTensor).floatBuffer
                val values = (result.get("value").get() as OnnxTensor).floatBuffer
                return batch.mapIndexed { row, f ->
                    val g = FloatArray(d).also { globOut.position(row * d); globOut.get(it) }
                    val n = f.entityCount
                    val ptr = Array(n + 2) { i ->
                        val at = if (i < n) i else e + (i - n)
                        FloatArray(d).also { pointer.position((row * (e + 2) + at) * d); pointer.get(it) }
                    }
                    val af = Array(f.actionCount) { i ->
                        FloatArray(3 * d).also { features.position((row * a + i) * 3 * d); features.get(it) }
                    }
                    val sc = FloatArray(f.actionCount) { scores.get(row * a + it) }
                    Encoded(g, ptr, af, sc, values.get(row))
                }
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    // -- the pick-at-a-time heads -----------------------------------------------------------------------------

    private fun w(name: String) = weights[name] ?: error("no weight $name")

    /** `nn.Linear`: y = W x + b, W [out, in] row-major. */
    private fun linear(name: String, x: DoubleArray, bias: Boolean = true): DoubleArray {
        val weight = w("$name.weight")
        val (out, inp) = shapes.getValue("$name.weight")
        require(x.size == inp) { "$name expects $inp inputs, got ${x.size}" }
        val b = if (bias) w("$name.bias") else null
        return DoubleArray(out) { o ->
            var s = b?.get(o)?.toDouble() ?: 0.0
            val base = o * inp
            for (i in 0 until inp) s += weight[base + i] * x[i]
            s
        }
    }

    private fun mlp(name: String, x: DoubleArray): DoubleArray =
        linear("$name.2", linear("$name.0", x).also { gelu(it) })

    private fun row(name: String, index: Int): DoubleArray {
        val (_, d) = shapes.getValue("$name.weight")
        val weight = w("$name.weight")
        return DoubleArray(d) { weight[index * d + it].toDouble() }
    }

    private fun slice(x: FloatArray, from: Int, to: Int) = DoubleArray(to - from) { x[from + it].toDouble() }

    /** `x_logits`: [xOut], masked entries at [NEG]. */
    fun xLogits(glob: FloatArray, chosen: FloatArray, mask: BooleanArray): DoubleArray {
        val d = width
        val logits = mlp("x_head", slice(chosen, 0, d) + slice(glob, 0, d))
        for (i in logits.indices) if (!mask[i]) logits[i] = NEG
        return logits
    }

    /** The target pointer head's per-slot state: keys are computed once per decision. */
    fun pointerKeys(pointer: Array<FloatArray>): Array<DoubleArray> =
        Array(pointer.size) { linear("key", slice(pointer[it], 0, width)) }

    /** `pointer_logits`: one logit per pointer token, then STOP. */
    fun pointerLogits(
        chosen: FloatArray, slot: Int, picked: BooleanArray, pickedSum: DoubleArray, keys: Array<DoubleArray>,
        allowed: BooleanArray, canStop: Boolean, canPick: Boolean,
    ): DoubleArray {
        val d = width
        val queryIn = slice(chosen, 0, d)
        val slotRow = row("slot", slot)
        val pick = linear("pick", pickedSum)
        for (i in 0 until d) queryIn[i] += slotRow[i] + pick[i]
        val query = mlp("query", queryIn + slice(chosen, 2 * d, 3 * d))
        val n = keys.size
        val out = DoubleArray(n + 1)
        val scale = sqrt(d.toDouble())
        for (j in 0 until n) {
            out[j] = if (allowed[j] && !picked[j] && canPick) dot(query, keys[j]) / scale else NEG
        }
        out[n] = if (canStop) linear("stop", query)[0] else NEG
        return out
    }

    /** `combat_pairs`: each source/target pair's embedding, with the exact combat-math features. */
    fun combatPairs(pointer: Array<FloatArray>, spec: CombatSpec): Array<DoubleArray> {
        val d = width
        return Array(spec.width) { i ->
            val (s, t) = if (i < spec.pairs.size) spec.pairs[i][0] to spec.pairs[i][1] else 0 to 0
            val pair = mlp("combat_pair", slice(pointer[s], 0, d) + slice(pointer[t], 0, d))
            val f = if (i < spec.pairs.size) spec.pairFeatures[i] else intArrayOf(0, 0, 0)
            val extra = linear("combat_pair_feature", DoubleArray(3) { symlog(f[it].toDouble()) }, bias = false)
            for (k in 0 until d) pair[k] += extra[k]
            pair
        }
    }

    /** `combat_logits` before `_combat`'s viability mask: one logit per pair, then STOP (both raw). */
    fun combatLogits(
        chosen: FloatArray, pairs: Array<DoubleArray>, attackersHead: Boolean, pickedSum: DoubleArray, step: IntArray,
        pairKeys: Array<DoubleArray>,
    ): DoubleArray {
        val d = width
        val queryIn = slice(chosen, 0, d)
        val kind = row("combat_kind", if (attackersHead) 1 else 2)
        val pick = linear("combat_pick", pickedSum)
        val stepFeature = linear("combat_step_feature", DoubleArray(2) { symlog(step[it].toDouble()) }, bias = false)
        for (i in 0 until d) queryIn[i] += kind[i] + pick[i] + stepFeature[i]
        val query = mlp("combat_query", queryIn + slice(chosen, 2 * d, 3 * d))
        val scale = sqrt(d.toDouble())
        val out = DoubleArray(pairs.size + 1)
        for (j in pairs.indices) out[j] = dot(query, pairKeys[j]) / scale
        out[pairs.size] = linear("combat_stop", query)[0]
        return out
    }

    fun combatKeys(pairs: Array<DoubleArray>): Array<DoubleArray> = Array(pairs.size) { linear("combat_key", pairs[it]) }

    override fun close() {
        session.close()
    }

    companion object {
        const val NEG = -1e9

        fun symlog(x: Double): Double = Math.signum(x) * ln(1.0 + abs(x))

        fun dot(a: DoubleArray, b: DoubleArray): Double {
            var s = 0.0
            for (i in a.indices) s += a[i] * b[i]
            return s
        }

        /** Exact GELU (`nn.GELU()`, approximate='none'): x/2 (1 + erf(x/sqrt 2)), in place. */
        fun gelu(x: DoubleArray) {
            for (i in x.indices) x[i] = 0.5 * x[i] * (1.0 + erf(x[i] / Math.sqrt(2.0)))
        }

        /** erf to ~1e-15: the Maclaurin series below 3, the erfc continued fraction above. */
        fun erf(z: Double): Double {
            val a = abs(z)
            val r = if (a < 3.0) {
                var term = a
                var sum = a
                var n = 0
                val z2 = a * a
                while (abs(term) > 1e-17 * abs(sum)) {
                    n++
                    term *= -z2 / n
                    sum += term / (2 * n + 1)
                    if (n > 200) break
                }
                2.0 / sqrt(Math.PI) * sum
            } else {
                // erfc(a) = exp(-a^2)/sqrt(pi) * 1/(a + 1/2/(a + 1/(a + 3/2/(a + ...)))) via modified Lentz.
                val tiny = 1e-300
                var f = a
                var c = a
                var dd = 0.0
                for (k in 1..300) {
                    val coef = k / 2.0
                    dd = a + coef * dd
                    dd = if (abs(dd) < tiny) tiny else dd
                    c = a + coef / c
                    c = if (abs(c) < tiny) tiny else c
                    dd = 1.0 / dd
                    val delta = c * dd
                    f *= delta
                    if (abs(delta - 1.0) < 1e-16) break
                }
                1.0 - exp(-a * a) / sqrt(Math.PI) / f
            }
            return if (z < 0) -r else r
        }

        fun logSoftmax(x: DoubleArray): DoubleArray {
            var max = Double.NEGATIVE_INFINITY
            for (v in x) if (v > max) max = v
            var s = 0.0
            for (v in x) s += exp(v - max)
            val lse = max + ln(s)
            return DoubleArray(x.size) { x[it] - lse }
        }
    }
}
