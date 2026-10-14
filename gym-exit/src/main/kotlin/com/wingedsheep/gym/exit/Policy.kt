package com.wingedsheep.gym.exit

import com.wingedsheep.gym.contract.EntityFeatures
import com.wingedsheep.gym.contract.LegalActionView
import com.wingedsheep.gym.contract.TrainingObservation
import java.util.BitSet
import java.util.SplittableRandom
import kotlin.math.exp

/** An observation featurized once, with the per-observation facts every head reads (`inference._Prepared`). */
class Prepared(val observation: TrainingObservation, val features: Features, xOut: Int) {
    val entities: Map<String, EntityFeatures> by lazy { Schema.entityFeatures(observation) }
    val observedAttacks: Map<String, String> by lazy { CombatMath.observedAttacks(observation) }
    val pointerIdSet: Set<String> by lazy { features.pointerIds.toSet() }

    /** `_supported`: legal actions whose every parameter a head can supply (no standalone mana abilities). */
    val supported: BooleanArray by lazy {
        BooleanArray(features.actionCount) { i ->
            val action = observation.legalActions[i]
            val heads = Schema.heads(action)
            var ok = Head.DAMAGE_DISTRIBUTION !in heads && !action.isManaAbility
            if (Head.TARGETS in heads) {
                ok = ok && if (Head.X_VALUE in heads) {
                    val low = maxOf(0, action.minX)
                    val high = minOf(action.maxAffordableX ?: 0, xOut - 1)
                    (low..high).any { Schema.targetsFeasible(action, entities, pointerIdSet, it) }
                } else {
                    Schema.targetsFeasible(action, entities, pointerIdSet, null)
                }
            }
            if (Head.BLOCKERS in heads && action.validBlockerAssignments.isNotEmpty()) {
                ok = ok && observedAttacks.isNotEmpty() && action.blockDeclarationConstraints != null
            }
            ok
        }
    }

    /** The action pointer's hard mask: affordable and supported. */
    val mask: BooleanArray by lazy {
        BooleanArray(features.actionCount) { features.actions[it][FeatureSpec.AFFORDABLE_COLUMN] != 0 && supported[it] }
    }
}

class Choice(
    val actionIndex: Int,
    val actionId: Int,
    val params: Params,
    val trace: IntArray,
    val logProbability: Double,
) {
    val key: String by lazy { "$actionIndex|${params.canonical()}" }
}

/** `_Picker` and `_FirstForced`: every stochastic choice of one decision, sampled, argmax'd or replayed. */
class Picker private constructor(
    private val rng: SplittableRandom?,
    private val forced: IntArray?,
    private val first: Int?,
) {
    val trace = ArrayList<Int>()
    var logProbability = 0.0
        private set

    fun pick(logits: DoubleArray): Int {
        val logP = PolicyNet.logSoftmax(logits)
        val index = when {
            first != null && trace.isEmpty() -> checked(first, logP)
            forced != null -> {
                if (trace.size >= forced.size) throw InferenceError("the stored trace ended before the decision did")
                checked(forced[trace.size], logP)
            }
            rng == null -> argmax(logits)
            else -> sample(logP, rng)
        }
        logProbability += logP[index]
        trace += index
        return index
    }

    fun finish() {
        if (forced != null && trace.size != forced.size) {
            throw InferenceError("the decision used ${trace.size} steps of a ${forced.size}-step trace")
        }
    }

    private fun checked(index: Int, logP: DoubleArray): Int {
        if (index !in logP.indices || !(logP[index] > PolicyNet.NEG / 2)) {
            throw InferenceError("the stored trace picks $index, which this step masks")
        }
        return index
    }

    companion object {
        fun argmax(): Picker = Picker(null, null, null)
        fun sample(rng: SplittableRandom): Picker = Picker(rng, null, null)
        fun forced(trace: IntArray): Picker = Picker(null, trace, null)
        fun firstForced(first: Int): Picker = Picker(null, null, first)

        /** torch.argmax: the first maximal entry. */
        fun argmax(x: DoubleArray): Int {
            var best = 0
            for (i in 1 until x.size) if (x[i] > x[best]) best = i
            return best
        }

        fun sample(logP: DoubleArray, rng: SplittableRandom): Int {
            var u = rng.nextDouble()
            var last = -1
            for (i in logP.indices) {
                val p = exp(logP[i])
                if (p <= 0.0) continue
                last = i
                u -= p
                if (u < 0) return i
            }
            return last
        }
    }
}

/** Port of `GameplayPolicy` (inference.py) over [PolicyNet]: the same masks, heads and pick order. */
class GameplayPolicy(val net: PolicyNet) {

    fun prepare(observation: TrainingObservation): Prepared {
        Schema.validateObservation(observation)
        val features = Featurizer.featurize(observation)
        if (features.actionCount == 0) throw InferenceError("the observation has no flat legal actions")
        return Prepared(observation, features, net.xOut)
    }

    fun encode(prepared: List<Prepared>): List<Encoded> = net.encode(prepared.map { it.features })

    /** The action head's masked logits ([PolicyNet.NEG] where masked). */
    fun actionLogits(prepared: Prepared, encoded: Encoded): DoubleArray {
        val mask = prepared.mask
        return DoubleArray(prepared.features.actionCount) {
            if (mask[it]) encoded.actionScores[it].toDouble() else PolicyNet.NEG
        }
    }

    /** One complete decision under [picker], or [InferenceError] when it cannot be answered. */
    fun decide(prepared: Prepared, encoded: Encoded, picker: Picker): Choice {
        if (prepared.mask.none { it }) {
            throw InferenceError("no affordable action has all parameters exposed to the policy")
        }
        val observation = prepared.observation
        val features = prepared.features
        val index = picker.pick(actionLogits(prepared, encoded))
        val action = observation.legalActions[index]
        val chosen = encoded.actionFeatures[index]
        val heads = Schema.heads(action)

        val convoke = convokePayment(prepared, action)
        val blight = blightTarget(prepared, action)
        val tapped = tapPayment(prepared, action)

        var xValue: Int? = null
        if (Head.X_VALUE in heads) {
            val low = maxOf(0, action.minX)
            val high = minOf(action.maxAffordableX ?: 0, net.xOut - 1)
            val mask = BooleanArray(net.xOut)
            for (v in low..high) mask[v] = true
            if (Head.TARGETS in heads) {
                for (v in low..high) {
                    if (!Schema.targetsFeasible(action, prepared.entities, prepared.pointerIdSet, v)) mask[v] = false
                }
            }
            xValue = picker.pick(net.xLogits(encoded.glob, chosen, mask))
        }

        val targets = if (Head.TARGETS in heads) targets(prepared, encoded, action, chosen, xValue, picker) else null

        var beheld: List<String>? = null
        if (action.beholdPaymentOptions.isNotEmpty()) {
            val selected = targets.orEmpty()
            if (selected.size != 1) throw InferenceError("Behold cast needs one selected target")
            val payment = beholdPayment(prepared, action, selected[0])
            if (payment.isEmpty()) throw InferenceError("selected Behold target has no engine-checked payment")
            beheld = payment
        }

        var attackers: Map<String, String>? = null
        var blockers: Map<String, List<String>>? = null
        if (Head.ATTACKERS in heads || Head.BLOCKERS in heads) {
            val spec = CombatSpec.of(observation, action, features.pointerIds, prepared.observedAttacks)
            val order = combat(encoded, spec, chosen, picker)
            if (spec.attackersHead) {
                attackers = LinkedHashMap<String, String>().also { m -> for ((a, t) in order) m[a] = t }
            } else {
                blockers = LinkedHashMap<String, MutableList<String>>().also { m ->
                    for ((b, a) in order) m.getOrPut(b) { mutableListOf() }.add(a)
                }
            }
        }
        val params = Params(
            convokePayments = convoke, blightTarget = blight, tappedPermanents = tapped, xValue = xValue,
            targets = targets, beheldCards = beheld, attackers = attackers, blockers = blockers,
        )
        Schema.validateActionParams(observation, action, params)
        return Choice(index, action.actionId, params, picker.trace.toIntArray(), picker.logProbability)
    }

    private fun targets(
        prepared: Prepared, encoded: Encoded, action: LegalActionView, chosen: FloatArray, xValue: Int?, picker: Picker,
    ): List<String> {
        val pointerIds = prepared.features.pointerIds
        val position = HashMap<String, Int>()
        for ((i, id) in pointerIds.withIndex()) position[id] = i
        val n = encoded.pointer.size
        val keys = net.pointerKeys(encoded.pointer)
        val selected = ArrayList<String>()
        for ((slotIndex, slot) in Schema.slots(action).withIndex()) {
            val allowed = BooleanArray(n)
            for (id in slot.candidates) {
                val at = position[id] ?: continue
                if (slot.mustDifferFromEarlier && id in selected) continue
                if (!Schema.xAllows(prepared.entities[id], xValue, slot.x)) continue
                allowed[at] = true
            }
            val low = slot.minTargets
            var high = slot.maxTargets
            if (slot.x.count && xValue != null) high = minOf(high, xValue)
            val picked = BooleanArray(n)
            val pickedSum = DoubleArray(net.width)
            for (step in 0..high) {
                val canStop = step >= low
                val canPick = step < high && (0 until n).any { allowed[it] && !picked[it] }
                val logits = net.pointerLogits(chosen, slotIndex, picked, pickedSum, keys, allowed, canStop, canPick)
                val index = picker.pick(logits)
                if (index == n) break
                selected += pointerIds[index]
                picked[index] = true
                val token = encoded.pointer[index]
                for (k in pickedSum.indices) pickedSum[k] += token[k].toDouble()
            }
        }
        return selected
    }

    private fun combat(encoded: Encoded, spec: CombatSpec, chosen: FloatArray, picker: Picker): List<Pair<String, String>> {
        val pairs = net.combatPairs(encoded.pointer, spec)
        val keys = net.combatKeys(pairs)
        val c = spec.width
        val picked = BitSet(c)
        val pickedSum = DoubleArray(net.width)
        val order = ArrayList<Int>()
        while (true) {
            val step = spec.stepFeatures(order)
            val (viable, canStop) = spec.viable(picked)
            val raw = net.combatLogits(chosen, pairs, spec.attackersHead, pickedSum, step, keys)
            val logits = DoubleArray(c + 1) { j ->
                when {
                    j < c -> if (viable[j]) raw[j] else Double.NEGATIVE_INFINITY
                    else -> if (canStop) raw[c] else Double.NEGATIVE_INFINITY
                }
            }
            if (logits.none { it.isFinite() }) throw InferenceError("combat declaration has no contract-valid completion")
            val index = picker.pick(logits)
            if (index == c) break
            if (index >= spec.pairIds.size || picked[index]) throw InferenceError("combat head selected a masked pair")
            order += index
            picked.set(index)
            for (k in pickedSum.indices) pickedSum[k] += pairs[index][k]
            if (order.size > spec.pairIds.size) throw InferenceError("combat head did not terminate")
        }
        return order.map { spec.pairIds[it] }
    }

    // -- the fixed payment rules (inference.py) --------------------------------------------------------------

    private fun power(prepared: Prepared, id: String) = maxOf(0, prepared.entities[id]?.power ?: 0)

    private fun convokePayment(prepared: Prepared, action: LegalActionView): Map<String, String?>? {
        val options = action.convokePaymentOptions
        if (options.isEmpty()) return null
        val best = options.minWithOrNull(
            compareBy<Map<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.scripting.ConvokePayment>>(
                { it.size }, { plan -> plan.keys.sumOf { power(prepared, it.value) } },
            ).thenComparator { a, b ->
                compareTuples(
                    a.entries.map { (k, v) -> listOf(k.value, v.color?.name ?: "") }.sortedWith(::compareLists),
                    b.entries.map { (k, v) -> listOf(k.value, v.color?.name ?: "") }.sortedWith(::compareLists),
                )
            },
        )!!
        // An empty plan is Python's `{}`: falsy, so no `convokePayments` key at all.
        if (best.isEmpty()) return null
        return LinkedHashMap<String, String?>().also { m -> for ((k, v) in best) m[k.value] = v.color?.name }
    }

    private fun blightTarget(prepared: Prepared, action: LegalActionView): String? {
        val options = action.blightTargetOptions
        if (options.isEmpty()) return null
        val amount = action.blightAmount
        fun toughness(id: String) = prepared.entities[id]?.toughness ?: 0
        return options.map { it.value }.minWithOrNull(
            compareBy<String>({ toughness(it) <= amount }, { power(prepared, it) }, { -toughness(it) }, { it }),
        )
    }

    private fun tapPayment(prepared: Prepared, action: LegalActionView): List<String>? {
        val options = action.tapPaymentOptions
        if (options.isEmpty()) return null
        val best = options.map { plan -> plan.map { it.value } }.minWithOrNull(
            compareBy<List<String>> { plan -> plan.sumOf { power(prepared, it) } }.thenComparator(::compareLists),
        )!!
        return best.ifEmpty { null }
    }

    private fun beholdPayment(prepared: Prepared, action: LegalActionView, target: String): List<String> {
        val options = action.beholdPaymentOptions[com.wingedsheep.sdk.model.EntityId(target)].orEmpty()
        if (options.isEmpty()) return emptyList()
        val battlefield = prepared.observation.zones.filter { it.zoneType.wire == "Battlefield" }
            .flatMap { z -> z.cards.map { it.entityId.value } }.toSet()
        return options.map { plan -> plan.map { it.value } }.minWithOrNull(
            compareBy<List<String>> { plan -> plan.count { it !in battlefield } }.thenComparator(::compareLists),
        )!!
    }

    // -- what the search asks of the policy ------------------------------------------------------------------

    /**
     * `top_actions`: the [k] likeliest distinct top-level actions, plus a legal pass if missing, each completed by
     * the argmax of the later heads, with the action head's log-probability for it.
     */
    fun topActions(prepared: Prepared, encoded: Encoded, k: Int): List<Pair<Choice, Double>> {
        val logits = actionLogits(prepared, encoded)
        val logP = PolicyNet.logSoftmax(logits)
        val allowed = prepared.mask.indices.filter { prepared.mask[it] }
            .sortedWith(compareBy<Int>({ -logP[it] }, { it }))
        if (allowed.isEmpty()) throw InferenceError("no affordable action has all parameters exposed to the policy")
        val chosen = allowed.take(k).toMutableList()
        val legal = prepared.observation.legalActions
        val pass = allowed.firstOrNull { legal[it].kind == "PassPriority" }
        if (pass != null && pass !in chosen) chosen += pass
        val out = ArrayList<Pair<Choice, Double>>()
        for (index in chosen) {
            try {
                out += decide(prepared, encoded, Picker.firstForced(index)) to logP[index]
            } catch (_: InferenceError) {
            }
        }
        return out
    }

    /** `evaluate` for one trace: replay it with a forced picker; the choice it rebuilds (with its log-probability). */
    fun replay(prepared: Prepared, encoded: Encoded, trace: IntArray): Choice {
        val picker = Picker.forced(trace)
        val choice = decide(prepared, encoded, picker)
        picker.finish()
        return choice
    }

    companion object {
        fun compareLists(a: List<String>, b: List<String>): Int {
            for (i in 0 until minOf(a.size, b.size)) {
                val c = a[i].compareTo(b[i])
                if (c != 0) return c
            }
            return a.size.compareTo(b.size)
        }

        fun compareTuples(a: List<List<String>>, b: List<List<String>>): Int {
            for (i in 0 until minOf(a.size, b.size)) {
                val c = compareLists(a[i], b[i])
                if (c != 0) return c
            }
            return a.size.compareTo(b.size)
        }
    }
}
