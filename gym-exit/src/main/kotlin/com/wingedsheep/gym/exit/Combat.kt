package com.wingedsheep.gym.exit

import com.wingedsheep.gym.contract.LegalActionView
import com.wingedsheep.gym.contract.TrainingObservation
import java.util.BitSet

/**
 * Port of mtg-draft-ai `gameplay/combat.py` (the rule-consequence calculator), `features._encode_combat`,
 * `bc_data._combat_steps` and `PolicyNet.combat_legal` / `combat_viable` for a single observation.
 */
class Fighter(
    val id: String,
    val controller: String?,
    val power: Int,
    val toughness: Int,
    val damage: Int,
    val keywords: Set<String>,
) {
    fun strikesIn(firstStep: Boolean): Boolean {
        if ("DOUBLE_STRIKE" in keywords) return true
        return ("FIRST_STRIKE" in keywords) == firstStep
    }
}

class CombatResult(val dead: Set<String>, val damageToPlayer: Map<String, Int>, val lifeGained: Map<String, Int>) {
    fun lifeChange(player: String): Int = (lifeGained[player] ?: 0) - (damageToPlayer[player] ?: 0)
}

object CombatMath {
    fun fighters(observation: TrainingObservation): LinkedHashMap<String, Fighter> {
        val out = LinkedHashMap<String, Fighter>()
        for (zone in observation.zones) {
            if (zone.zoneType.wire != "Battlefield") continue
            for (card in zone.cards) {
                val power = card.power ?: continue
                val toughness = card.toughness ?: continue
                out[card.entityId.value] = Fighter(card.entityId.value, card.controllerId?.value, power, toughness,
                    card.damageMarked, card.keywords)
            }
        }
        return out
    }

    /** `attacks_from_observation`: gym v1.9 combat state, attacker -> what it attacks, in observation order. */
    fun observedAttacks(observation: TrainingObservation): LinkedHashMap<String, String> {
        val out = LinkedHashMap<String, String>()
        for (zone in observation.zones) {
            if (zone.zoneType.wire != "Battlefield") continue
            for (card in zone.cards) {
                if (!card.attacking) continue
                val target = card.attackTargetId?.value ?: continue
                out[card.entityId.value] = target
            }
        }
        return out
    }

    private fun dies(f: Fighter, damage: Int, deathtouched: Boolean): Boolean {
        if (f.toughness <= 0) return true
        if ("INDESTRUCTIBLE" in f.keywords) return false
        return damage >= f.toughness || deathtouched
    }

    private class DamageStep(
        val board: Map<String, Fighter>,
        val damage: Map<String, Int>,
        val toPlayer: HashMap<String, Int>,
        val gained: HashMap<String, Int>,
    ) {
        val dealt = LinkedHashMap<String, Int>()
        val touched = HashSet<String>()

        fun hit(source: Fighter, target: String, amount: Int) {
            if (amount <= 0) return
            dealt[target] = (dealt[target] ?: 0) + amount
            if ("DEATHTOUCH" in source.keywords) touched += target
            if ("LIFELINK" in source.keywords) gained.merge(source.controller ?: "null", amount, Int::plus)
        }

        fun hitPlayer(source: Fighter, target: String, amount: Int) {
            if (target in board) return
            toPlayer.merge(target, amount, Int::plus)
            if ("LIFELINK" in source.keywords) gained.merge(source.controller ?: "null", amount, Int::plus)
        }

        fun need(attacker: Fighter, blocker: String): Int {
            if ("DEATHTOUCH" in attacker.keywords) return 1
            val left = board.getValue(blocker).toughness - damage.getValue(blocker) - (dealt[blocker] ?: 0)
            return maxOf(left, 0)
        }

        fun attack(attacker: Fighter, target: String, alive: List<String>, blocked: Boolean) {
            val power = maxOf(attacker.power, 0)
            if (!blocked) {
                hitPlayer(attacker, target, power)
                return
            }
            if (alive.isEmpty()) {
                if ("TRAMPLE" in attacker.keywords) hitPlayer(attacker, target, power)
                return
            }
            var left = power
            for (blocker in alive.sortedBy { need(attacker, it) }) {
                val amount = minOf(need(attacker, blocker), left)
                hit(attacker, blocker, amount)
                left -= amount
            }
            if (left > 0) {
                if ("TRAMPLE" in attacker.keywords) hitPlayer(attacker, target, left) else hit(attacker, alive[0], left)
            }
        }
    }

    fun resolve(board: Map<String, Fighter>, attacks: Map<String, String>, blocks: Map<String, List<String>>): CombatResult {
        val blockedBy = LinkedHashMap<String, MutableList<String>>()
        for (attacker in attacks.keys) blockedBy[attacker] = mutableListOf()
        val blocking = LinkedHashMap<String, String>()
        for ((blocker, list) in blocks) {
            val attackers = list.filter { it in attacks }
            if (blocker in board && attackers.isNotEmpty()) {
                blocking[blocker] = attackers[0]
                for (a in attackers) blockedBy.getValue(a).add(blocker)
            }
        }
        val damage = LinkedHashMap<String, Int>()
        for (fid in attacks.keys + blocking.keys) board[fid]?.let { damage[fid] = it.damage }
        val deathtouched = HashSet<String>()
        val dead = LinkedHashSet<String>()
        val toPlayer = HashMap<String, Int>()
        val gained = HashMap<String, Int>()
        for (firstStep in listOf(true, false)) {
            val hits = DamageStep(board, damage, toPlayer, gained)
            for ((attackerId, target) in attacks) {
                val attacker = board[attackerId] ?: continue
                if (attackerId in dead || !attacker.strikesIn(firstStep)) continue
                val by = blockedBy.getValue(attackerId)
                hits.attack(attacker, target, by.filter { it !in dead }, blocked = by.isNotEmpty())
            }
            for ((blockerId, attackerId) in blocking) {
                val blocker = board.getValue(blockerId)
                if (blockerId in dead || attackerId in dead || !blocker.strikesIn(firstStep)) continue
                hits.hit(blocker, attackerId, maxOf(blocker.power, 0))
            }
            for ((fid, amount) in hits.dealt) {
                // Python's `damage[fid] += amount` raises on an id it never seeded; so does this.
                damage[fid] = (damage[fid] ?: throw InferenceError("combat calculator: unseeded $fid")) + amount
            }
            deathtouched += hits.touched
            for ((fid, total) in damage) {
                if (fid !in dead && dies(board.getValue(fid), total, fid in deathtouched)) dead += fid
            }
        }
        return CombatResult(dead, toPlayer, gained)
    }

    /** `block_pair`: (blocker dies, attacker dies, trample through) for one pair taken alone. */
    fun blockPair(board: Map<String, Fighter>, attacker: String, blocker: String): IntArray {
        val result = resolve(board, mapOf(attacker to "_player"), mapOf(blocker to listOf(attacker)))
        return intArrayOf(if (blocker in result.dead) 1 else 0, if (attacker in result.dead) 1 else 0,
            result.damageToPlayer["_player"] ?: 0)
    }
}

/** One observation's combat declaration as the combat head sees it (`CombatLabel` plus its collated tensors). */
class CombatSpec(
    val attackersHead: Boolean,
    val pointerCount: Int,
    val pairs: List<IntArray>,
    val pairIds: List<Pair<String, String>>,
    val requiredPairs: BooleanArray,
    val requiredSources: BooleanArray,
    val sourceLimits: IntArray,
    val targetMin: IntArray,
    val targetMax: IntArray,
    val maxSources: Int,
    val coAlternatives: Array<Array<BooleanArray>>,
    val coGroupMask: Array<BooleanArray>,
    val pairFeatures: List<IntArray>,
    val board: Map<String, Fighter>,
    val baseAttacks: Map<String, String>,
    val playerId: String,
    val lifeTotal: Int,
) {
    /** Collated width: `max(1, pairs)`, so the STOP index is this too. */
    val width: Int = maxOf(1, pairs.size)
    private val anyTargetMin = targetMin.any { it != 0 }
    private val anyCo = coGroupMask.any { row -> row.any { it } }

    private fun src(i: Int) = if (i < pairs.size) pairs[i][0] else 0
    private fun tgt(i: Int) = if (i < pairs.size) pairs[i][1] else 0
    private fun real(i: Int) = i < pairs.size

    /** `combat_legal` for one row: the pairs still legal given [picked], and whether STOP is. */
    fun legal(picked: BitSet): Pair<BooleanArray, Boolean> {
        val c = width
        val p = pointerCount
        val legal = BooleanArray(c)
        val sourceCounts = IntArray(c)
        val targetCounts = IntArray(c)
        val missingRequired = IntArray(c)
        for (i in 0 until c) for (j in 0 until c) {
            if (src(i) == src(j)) {
                if (picked[j]) sourceCounts[i]++
                if (real(j) && requiredPairs[j] && !picked[j]) missingRequired[i]++
            }
            if (tgt(i) == tgt(j) && picked[j]) targetCounts[i]++
        }
        val selected = BooleanArray(p)
        for (j in 0 until c) if (picked[j]) selected[src(j)] = true
        val selectedCount = selected.count { it }
        val belowCap = maxSources < 0 || selectedCount < maxSources
        for (i in 0 until c) {
            if (!real(i) || picked[i]) continue
            val limit = sourceLimits[i]
            var ok = sourceCounts[i] < limit && targetCounts[i] < targetMax[tgt(i)]
            val missingAfter = missingRequired[i] - (if (requiredPairs[i]) 1 else 0)
            ok = ok && sourceCounts[i] + 1 + missingAfter <= limit
            ok = ok && (selected[src(i)] || belowCap)
            legal[i] = ok
        }
        if (anyTargetMin) {
            val before = legal.copyOf()
            for (i in 0 until c) {
                if (!legal[i]) continue
                val minimum = targetMin[tgt(i)]
                if (minimum <= 1) continue
                val sources = BooleanArray(p)
                for (j in 0 until c) if (tgt(i) == tgt(j) && (before[j] || picked[j])) sources[src(j)] = true
                if (sources.count { it } < minimum) legal[i] = false
            }
        }
        if (anyCo) {
            val available = selected.copyOf()
            for (j in 0 until c) if (legal[j]) available[src(j)] = true
            for (i in 0 until c) {
                if (!legal[i]) continue
                for (g in coGroupMask[i].indices) {
                    if (!coGroupMask[i][g]) continue
                    if ((0 until p).none { coAlternatives[i][g][it] && available[it] }) { legal[i] = false; break }
                }
            }
        }
        var canStop = (0 until c).all { !real(it) || !requiredPairs[it] || picked[it] }
        canStop = canStop && (0 until p).all { !requiredSources[it] || selected[it] }
        if (canStop && anyTargetMin) {
            val counts = IntArray(p)
            for (j in 0 until c) if (picked[j]) counts[tgt(j)]++
            canStop = (0 until p).all { counts[it] == 0 || counts[it] >= targetMin[it] }
        }
        if (canStop && anyCo) {
            for (i in 0 until c) {
                if (!selected[src(i)]) continue
                for (g in coGroupMask[i].indices) {
                    if (!coGroupMask[i][g]) continue
                    if ((0 until p).none { coAlternatives[i][g][it] && selected[it] }) { canStop = false; break }
                }
                if (!canStop) break
            }
        }
        return legal to canStop
    }

    /** `combat_viable`: legal picks whose suffix can still reach a contract-valid STOP (memoized search). */
    fun viable(picked: BitSet): Pair<BooleanArray, Boolean> {
        val (legal, canStop) = legal(picked)
        val memo = HashMap<BitSet, Boolean>()
        fun canFinish(bits: BitSet): Boolean {
            memo[bits]?.let { return it }
            val (suffixLegal, suffixStop) = legal(bits)
            if (suffixStop) { memo[bits] = true; return true }
            for (index in pairs.indices) {
                if (!suffixLegal[index]) continue
                val next = bits.clone() as BitSet
                next.set(index)
                if (canFinish(next)) { memo[bits] = true; return true }
            }
            memo[bits] = false
            return false
        }
        val viable = legal.copyOf()
        for (index in pairs.indices) {
            if (!legal[index]) continue
            val next = picked.clone() as BitSet
            next.set(index)
            viable[index] = canFinish(next)
        }
        return viable to canStop
    }

    /** The last row of `_combat_steps(combat, order)`: (damage, life) if the declaration stopped now. */
    fun stepFeatures(order: List<Int>): IntArray {
        val attacks = LinkedHashMap(baseAttacks)
        val blocks = LinkedHashMap<String, MutableList<String>>()
        for (index in order) {
            val (source, target) = pairIds[index]
            if (attacksHeadWrites()) attacks[source] = target else blocks.getOrPut(source) { mutableListOf() }.add(target)
        }
        val result = CombatMath.resolve(board, attacks, blocks)
        val damage = if (!attackersHead) {
            result.damageToPlayer[playerId] ?: 0
        } else {
            result.damageToPlayer.filterKeys { it != playerId }.values.sum()
        }
        return intArrayOf(damage, lifeTotal + result.lifeChange(playerId))
    }

    private fun attacksHeadWrites() = attackersHead

    companion object {
        const val MAX_CO_GROUPS = 4

        /** `_encode_combat` with no chosen pairs, as inference calls it. */
        fun of(
            observation: TrainingObservation, action: LegalActionView, pointerIds: List<String>,
            observedAttacks: Map<String, String>,
        ): CombatSpec {
            val heads = Schema.heads(action)
            val me = observation.perspectivePlayerId.value
            val player = observation.players.first { it.id.value == me }
            val board = CombatMath.fighters(observation)
            val pointer = HashMap<String, Int>()
            for ((i, id) in pointerIds.withIndex()) pointer[id] = i
            val p = pointerIds.size
            val pairIds = ArrayList<Pair<String, String>>()
            val requiredSourcesList = ArrayList<Int>()
            val limitsBySource = HashMap<String, Int>()
            val mandatoryPairs = HashSet<Pair<String, String>>()
            val attackersHead = Head.ATTACKERS in heads
            val baseAttacks: Map<String, String>
            if (attackersHead) {
                val validAttackers = action.validAttackers.map { it.value }.filter { it in pointer }
                val validTargets = action.validAttackTargets.map { it.value }.filter { it in pointer }
                for (a in validAttackers) for (t in validTargets) pairIds += a to t
                val mandatory = action.mandatoryAttackers.map { it.value }.toSet()
                for (a in validAttackers) if (a in mandatory) requiredSourcesList += pointer.getValue(a)
                for (a in validAttackers) limitsBySource[a] = 1
                baseAttacks = emptyMap()
            } else {
                for ((blocker, offered) in action.validBlockerAssignments) {
                    if (blocker.value !in pointer) continue
                    for (attacker in offered) if (attacker.value in pointer) pairIds += blocker.value to attacker.value
                }
                for ((blocker, assigned) in action.mandatoryBlockerAssignments) {
                    for (attacker in assigned) mandatoryPairs += blocker.value to attacker.value
                }
                val limits = action.blockerMaxBlockCounts.mapKeys { it.key.value }
                for (blocker in action.validBlockerAssignments.keys) limitsBySource[blocker.value] = limits[blocker.value] ?: 1
                baseAttacks = LinkedHashMap(observedAttacks)
            }
            val constraints = action.blockDeclarationConstraints
            val c = maxOf(1, pairIds.size)
            val targetMin = IntArray(p)
            val targetMax = IntArray(p) { c + 1 }
            constraints?.attackerMinBlockCounts?.forEach { (a, n) -> pointer[a.value]?.let { targetMin[it] = n } }
            constraints?.attackerMaxBlockCounts?.forEach { (a, n) -> pointer[a.value]?.let { targetMax[it] = n } }
            val coRequirements = HashMap<Int, List<List<Int>>>()
            constraints?.blockerCoRequirements?.forEach { (blocker, groups) ->
                val b = pointer[blocker.value] ?: return@forEach
                coRequirements[b] = groups.map { alts -> alts.mapNotNull { pointer[it.value] } }
            }
            val pairs = pairIds.map { (s, t) -> intArrayOf(pointer.getValue(s), pointer.getValue(t)) }
            val requiredPairs = BooleanArray(c)
            if (!attackersHead) for ((i, pair) in pairIds.withIndex()) if (pair in mandatoryPairs) requiredPairs[i] = true
            val requiredSources = BooleanArray(p)
            for (s in requiredSourcesList) requiredSources[s] = true
            val coAlternatives = Array(c) { Array(MAX_CO_GROUPS) { BooleanArray(p) } }
            val coGroupMask = Array(c) { BooleanArray(MAX_CO_GROUPS) }
            for ((i, pair) in pairs.withIndex()) {
                val groups = coRequirements[pair[0]].orEmpty()
                if (groups.size > MAX_CO_GROUPS) throw InferenceError("blocker has ${groups.size} co-block requirements")
                for ((g, alternatives) in groups.withIndex()) {
                    coGroupMask[i][g] = true
                    for (a in alternatives) coAlternatives[i][g][a] = true
                }
            }
            val pairFeatures = pairIds.map { (source, target) ->
                if (!attackersHead && source in board && target in board) CombatMath.blockPair(board, target, source)
                else intArrayOf(0, 0, 0)
            }
            val sourceLimits = IntArray(c)
            for ((i, pair) in pairIds.withIndex()) sourceLimits[i] = limitsBySource.getValue(pair.first)
            return CombatSpec(
                attackersHead = attackersHead, pointerCount = p, pairs = pairs, pairIds = pairIds,
                requiredPairs = requiredPairs, requiredSources = requiredSources, sourceLimits = sourceLimits,
                targetMin = targetMin, targetMax = targetMax, maxSources = constraints?.maxBlockingCreatures ?: -1,
                coAlternatives = coAlternatives, coGroupMask = coGroupMask, pairFeatures = pairFeatures,
                board = board, baseAttacks = baseAttacks, playerId = me, lifeTotal = player.lifeTotal,
            )
        }
    }
}
