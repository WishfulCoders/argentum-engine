package com.wingedsheep.gym.exit

import com.wingedsheep.gym.contract.ActionParams
import com.wingedsheep.gym.contract.EntityFeatures
import com.wingedsheep.gym.contract.LegalActionView
import com.wingedsheep.gym.contract.TargetRequirementView
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConvokePayment

/** The observation or a constructed choice cannot be answered by the policy (Python's `InferenceError`). */
class InferenceError(message: String) : RuntimeException(message, null, false, false)

/** Port of mtg-draft-ai `gameplay/schema.py`: the parameter heads and the checks `inference.py` relies on. */
enum class Head { TARGETS, X_VALUE, ATTACKERS, BLOCKERS, DAMAGE_DISTRIBUTION }

object Schema {
    /** The gym contracts `schema.py` accepts that a training-line jar can emit. */
    val SUPPORTED = setOf(
        "argentum-gym-contract@v1.5-complete-zones", "argentum-gym-contract@v1.6-policy-grounding",
        "argentum-gym-contract@v1.7-action-parameters", "argentum-gym-contract@v1.8-structured-decisions",
        "argentum-gym-contract@v1.9-combat-state", "argentum-gym-contract@v1.10-convoke-options",
        "argentum-gym-contract@v1.11-blight-options", "argentum-gym-contract@v1.12-tap-options",
        "argentum-gym-contract@v1.13-behold-options", "argentum-gym-contract@v1.14-loop-shortcut",
    )
    private val ZONE_ORDER = listOf("Hand", "Library", "Graveyard", "Exile", "Battlefield", "Command", "Sideboard")
    private val CONVOKE_ON = SUPPORTED.drop(5).toSet()
    private val BLIGHT_ON = SUPPORTED.drop(6).toSet()
    private val TAP_ON = SUPPORTED.drop(7).toSet()
    private val BEHOLD_ON = SUPPORTED.drop(8).toSet()

    fun heads(action: LegalActionView): Set<Head> {
        if (action.isDecisionOption || action.kind == "DECISION") return emptySet()
        if (action.kind == "DeclareAttackers") return setOf(Head.ATTACKERS)
        if (action.kind == "DeclareBlockers") return setOf(Head.BLOCKERS)
        val out = LinkedHashSet<Head>()
        if (action.targetEntityIds.isNotEmpty() || action.targetRequirements.isNotEmpty()) out += Head.TARGETS
        if (action.hasXCost) out += Head.X_VALUE
        if (action.requiresDamageDistribution) out += Head.DAMAGE_DISTRIBUTION
        return out
    }

    fun picksMore(action: LegalActionView): Boolean =
        heads(action).any { it == Head.X_VALUE || it == Head.TARGETS || it == Head.ATTACKERS || it == Head.BLOCKERS }

    /** `validate_observation`, reduced to what a live training-line observation can violate. */
    fun validateObservation(observation: TrainingObservation) {
        val hash = observation.schemaHash
        if (hash !in SUPPORTED) throw InferenceError("invalid observation: gym schema mismatch: $hash")
        val players = observation.players.map { it.id.value }
        if (players.size != players.toSet().size) throw InferenceError("invalid observation: duplicate players")
        if (observation.perspectivePlayerId.value !in players) throw InferenceError("invalid observation: perspective")
        val expected = players.flatMap { p -> ZONE_ORDER.map { p to it } }
        val actual = observation.zones.map { it.ownerId.value to it.zoneType.wire }
        if (actual != expected) throw InferenceError("invalid observation: zones does not follow player order")
        for (zone in observation.zones) {
            if (zone.size < zone.cards.size) throw InferenceError("invalid observation: zone size")
        }
        val ids = observation.legalActions.map { it.actionId }
        if (ids.size != ids.toSet().size) throw InferenceError("invalid observation: duplicate actionIds")
        val pending = observation.pendingDecision
        if (pending != null && pending.requiresStructuredResponse && observation.legalActions.isNotEmpty()) {
            throw InferenceError("invalid observation: structured decision with flat actions")
        }
        for (action in observation.legalActions) {
            if (action.isDecisionOption != (action.kind == "DECISION")) {
                throw InferenceError("invalid observation: decision actions must use kind=DECISION")
            }
            if (action.kind == "RepeatLoop") throw InferenceError("invalid observation: RepeatLoop needs v1.14")
            if (hash in CONVOKE_ON && action.affordable && action.hasConvoke && !action.canPayWithoutConvoke &&
                action.convokePaymentOptions.isEmpty()
            ) throw InferenceError("invalid observation: callable convoke-dependent cast has no payment option")
            if (hash in BLIGHT_ON && action.affordable && action.blightAmount > 0 && action.blightTargetOptions.isEmpty()) {
                throw InferenceError("invalid observation: callable Blight activation has no target option")
            }
            if (hash in TAP_ON && action.affordable && action.tapCount > 0 && action.tapPaymentOptions.isEmpty()) {
                throw InferenceError("invalid observation: callable TapPermanents activation has no payment option")
            }
            if (hash in BEHOLD_ON && action.affordable && action.beholdCount > 0) {
                val targets = action.targetEntityIds.toSet()
                val options = action.beholdPaymentOptions
                if (targets.isEmpty() || options.keys != targets || options.values.any { it.isEmpty() }) {
                    throw InferenceError("invalid observation: callable Behold cast lacks a payment for every target")
                }
            }
        }
    }

    /** `_target_features`: every zone card by entity id (the stack is not a zone). */
    fun entityFeatures(observation: TrainingObservation): Map<String, EntityFeatures> {
        val out = HashMap<String, EntityFeatures>()
        for (zone in observation.zones) for (card in zone.cards) out[card.entityId.value] = card
        return out
    }

    fun xAllows(card: EntityFeatures?, xValue: Int?, c: XConstraints): Boolean {
        if (!c.manaValue && !c.manaValueExactly && !c.power) return true
        if (xValue == null) return false
        // `card.get("manaValue", x + 1)`: a card absent from the zones has no manaValue and fails the bound.
        if (c.manaValue && (card?.manaValue ?: (xValue + 1)) > xValue) return false
        if (c.manaValueExactly && card?.manaValue != xValue) return false
        return !(c.power && card?.power != xValue)
    }

    /** One target slot as `_targets` and `_targets_feasible` read it: a requirement, or the action itself. */
    class Slot(
        val candidates: List<String>,
        val minTargets: Int,
        val maxTargets: Int,
        val mustDifferFromEarlier: Boolean,
        val x: XConstraints,
    )

    class XConstraints(val manaValue: Boolean, val manaValueExactly: Boolean, val power: Boolean, val count: Boolean)

    fun slots(action: LegalActionView): List<Slot> =
        if (action.targetRequirements.isNotEmpty()) {
            action.targetRequirements.map { r: TargetRequirementView ->
                Slot(r.targetEntityIds.map { it.value }, r.minTargets, r.maxTargets, r.mustDifferFromEarlier,
                    XConstraints(r.xConstrainsManaValue, r.xConstrainsManaValueExactly, r.xConstrainsPower, r.xConstrainsCount))
            }
        } else {
            listOf(Slot(action.targetEntityIds.map { it.value }, action.minTargets, action.maxTargets, false,
                XConstraints(action.xConstrainsTargetManaValue, action.xConstrainsTargetManaValueExactly,
                    action.xConstrainsTargetPower, action.xConstrainsTargetCount)))
        }

    /** `_targets_feasible`: can every slot meet its minimum at this X? */
    fun targetsFeasible(
        action: LegalActionView, entities: Map<String, EntityFeatures>, pointerIds: Set<String>, xValue: Int?,
    ): Boolean {
        for (slot in slots(action)) {
            val candidates = slot.candidates.filter { it in pointerIds && xAllows(entities[it], xValue, slot.x) }
            var high = slot.maxTargets
            if (slot.x.count && xValue != null) high = minOf(high, xValue)
            if (high < slot.minTargets || candidates.toSet().size < slot.minTargets) return false
        }
        return true
    }

    // -- validate_action_params -------------------------------------------------------------------------------

    fun validateActionParams(observation: TrainingObservation, action: LegalActionView, p: Params) {
        fun fail(message: String): Nothing = throw InferenceError("policy constructed invalid ActionParams: $message")
        if (observation.legalActions.count { it.actionId == action.actionId } != 1) fail("actionId occurs twice")
        if (!action.affordable) fail("actionId ${action.actionId} is present but not affordable")

        val convoke = p.convokePayments
        if (!convoke.isNullOrEmpty()) {
            if (!action.hasConvoke) fail("params.convokePayments requires a convoke action")
            val eligible = action.validConvokeCreatures.associate { it.entityId.value to it.colors.toSet() }
            for ((creature, color) in convoke) {
                val colors = eligible[creature] ?: fail("convoke creature $creature is not eligible")
                if (color != null && color !in colors) fail("convoke creature $creature cannot pay $color")
            }
        }
        val options = action.convokePaymentOptions
        if (action.affordable && action.hasConvoke && !action.canPayWithoutConvoke && options.isNotEmpty() &&
            convoke.isNullOrEmpty()
        ) fail("convoke-dependent cast requires params.convokePayments")
        if (options.isNotEmpty() && options.none { convokeMatches(it, convoke.orEmpty()) }) {
            fail("params.convokePayments must match an engine-checked option")
        }

        val blight = p.blightTarget
        if (blight != null) {
            if (action.kind != "ActivateAbility" || action.blightTargetOptions.none { it.value == blight }) {
                fail("params.blightTarget must match an engine-checked option")
            }
        } else if (action.blightTargetOptions.isNotEmpty()) fail("Blight activation requires params.blightTarget")

        val tapped = p.tappedPermanents.orEmpty()
        if (tapped.isNotEmpty()) {
            if (action.kind != "ActivateAbility" || action.tapPaymentOptions.none { o -> o.map { it.value } == tapped }) {
                fail("params.tappedPermanents must match an engine-checked option")
            }
        } else if (action.tapPaymentOptions.isNotEmpty()) fail("TapPermanents activation requires params.tappedPermanents")

        val beheld = p.beheldCards.orEmpty()
        if (beheld.isNotEmpty()) {
            val selected = p.targets.orEmpty()
            val plans = if (selected.size == 1) action.beholdPaymentOptions[EntityId(selected[0])].orEmpty() else emptyList()
            if (selected.size != 1 || plans.none { o -> o.map { it.value } == beheld }) {
                fail("params.beheldCards must match an engine-checked target/payment pair")
            }
        } else if (action.beholdPaymentOptions.isNotEmpty()) fail("Behold cast requires params.beheldCards")

        val heads = heads(action)
        val populated = HashSet<Head>()
        val targets = p.targets.orEmpty()
        if (targets.isNotEmpty()) populated += Head.TARGETS
        val x = p.xValue
        if (x != null) populated += Head.X_VALUE
        if (x != null && Head.X_VALUE in heads) {
            val maximum = action.maxAffordableX
            if (x < action.minX || maximum == null || x > maximum) fail("params.xValue $x is outside [${action.minX}, $maximum]")
        }
        if (Head.X_VALUE in heads && x == null) fail("an X action requires params.xValue")

        if (Head.TARGETS in heads) {
            if (action.targetRequirements.isNotEmpty()) {
                if (splitTargets(observation, action.targetRequirements, targets, x) == null) {
                    fail("params.targets does not satisfy targetRequirements")
                }
            } else {
                val candidates = action.targetEntityIds.map { it.value }.toSet()
                if (!candidates.containsAll(targets)) fail("params.targets contains an id outside targetEntityIds")
                var maxTargets = action.maxTargets
                if (action.xConstrainsTargetCount && x != null) maxTargets = minOf(maxTargets, x)
                if (targets.size < action.minTargets || targets.size > maxTargets) fail("target count")
                val c = XConstraints(action.xConstrainsTargetManaValue, action.xConstrainsTargetManaValueExactly,
                    action.xConstrainsTargetPower, false)
                if (!targetsMatchX(observation, targets, x, c)) fail("params.targets violates the selected X value")
            }
        }

        val attackers = p.attackers.orEmpty()
        if (attackers.isNotEmpty()) populated += Head.ATTACKERS
        if (Head.ATTACKERS in heads) {
            val valid = action.validAttackers.map { it.value }.toSet()
            val validTargets = action.validAttackTargets.map { it.value }.toSet()
            if (!valid.containsAll(attackers.keys)) fail("params.attackers contains an id outside validAttackers")
            if (!validTargets.containsAll(attackers.values)) fail("params.attackers contains a defender outside validAttackTargets")
            if (!attackers.keys.containsAll(action.mandatoryAttackers.map { it.value })) fail("params.attackers omits a mandatory attacker")
        }

        val blockers = p.blockers.orEmpty()
        if (blockers.isNotEmpty()) populated += Head.BLOCKERS
        if (Head.BLOCKERS in heads) {
            val valid = action.validBlockers.map { it.value }.toSet()
            if (!valid.containsAll(blockers.keys)) fail("params.blockers contains an id outside validBlockers")
            val limits = action.blockerMaxBlockCounts.mapKeys { it.key.value }
            for ((blocker, assigned) in blockers) {
                if (assigned.size > (limits[blocker] ?: 1)) fail("blocker $blocker exceeds its block-count limit")
            }
            val pairwise = action.validBlockerAssignments.entries.associate { (k, v) -> k.value to v.map { it.value }.toSet() }
            if (pairwise.isNotEmpty()) {
                for ((blocker, assigned) in blockers) {
                    if (!(pairwise[blocker] ?: emptySet()).containsAll(assigned)) fail("params.blockers contains an attacker $blocker cannot block")
                }
            }
            for ((blocker, required) in action.mandatoryBlockerAssignments) {
                if (!blockers[blocker.value].orEmpty().toSet().containsAll(required.map { it.value })) {
                    fail("params.blockers omits a mandatory assignment for ${blocker.value}")
                }
            }
            val constraints = action.blockDeclarationConstraints
            if (constraints != null) {
                val counts = HashMap<String, Int>()
                for (assigned in blockers.values) for (a in assigned) counts[a] = (counts[a] ?: 0) + 1
                val minimums = constraints.attackerMinBlockCounts.mapKeys { it.key.value }
                val maximums = constraints.attackerMaxBlockCounts.mapKeys { it.key.value }
                if (minimums.values.any { it < 1 }) fail("attackerMinBlockCounts values must be positive integers")
                if (maximums.values.any { it < 0 }) fail("attackerMaxBlockCounts values must be nonnegative integers")
                for ((attacker, count) in counts) {
                    if (count != 0 && count < (minimums[attacker] ?: 1)) fail("attacker $attacker has too few blockers")
                    if (count > (maximums[attacker] ?: count)) fail("attacker $attacker has too many blockers")
                }
                val cap = constraints.maxBlockingCreatures
                if (cap != null && (cap < 0 || blockers.size > cap)) fail("params.blockers exceeds maxBlockingCreatures")
                val co = constraints.blockerCoRequirements.mapKeys { it.key.value }
                for (blocker in blockers.keys) {
                    for (group in co[blocker].orEmpty()) {
                        if (group.none { it.value != blocker && it.value in blockers.keys }) {
                            fail("blocker $blocker has no required co-blocker")
                        }
                    }
                }
            }
        }
        if (Head.DAMAGE_DISTRIBUTION in heads) fail("this action requires params.damageDistribution")
        val inapplicable = populated - heads
        if (inapplicable.isNotEmpty()) fail("params populate inapplicable head(s): $inapplicable")
    }

    private fun convokeMatches(option: Map<EntityId, ConvokePayment>, chosen: Map<String, String?>): Boolean =
        option.size == chosen.size && option.all { (id, payment) ->
            chosen.containsKey(id.value) && chosen[id.value] == payment.color?.name
        }

    /** `split_targets`: the flattened targets cut into requirement slots, or null when no cut satisfies them. */
    fun splitTargets(
        observation: TrainingObservation, requirements: List<TargetRequirementView>, targets: List<String>, xValue: Int?,
    ): List<List<String>>? {
        val chunks = ArrayList<List<String>>()
        fun matches(r: TargetRequirementView, chosen: List<String>, earlier: Set<String>): Boolean {
            val candidates = r.targetEntityIds.map { it.value }.toSet()
            if (!candidates.containsAll(chosen) || chosen.toSet().size != chosen.size) return false
            if (r.mustDifferFromEarlier && chosen.any { it in earlier }) return false
            if (r.xConstrainsCount && (xValue == null || chosen.size > xValue)) return false
            return targetsMatchX(observation, chosen, xValue,
                XConstraints(r.xConstrainsManaValue, r.xConstrainsManaValueExactly, r.xConstrainsPower, false))
        }
        fun partitions(index: Int, offset: Int, earlier: Set<String>): Boolean {
            if (index == requirements.size) return offset == targets.size
            val r = requirements[index]
            var maximum = r.maxTargets
            if (r.xConstrainsCount && xValue != null) maximum = minOf(maximum, xValue)
            for (count in r.minTargets..maximum) {
                if (offset + count > targets.size) continue
                val chosen = targets.subList(offset, offset + count)
                if (!matches(r, chosen, earlier)) continue
                chunks += chosen
                if (partitions(index + 1, offset + count, earlier + chosen)) return true
                chunks.removeAt(chunks.size - 1)
            }
            return false
        }
        return if (partitions(0, 0, emptySet())) chunks else null
    }

    private fun targetsMatchX(observation: TrainingObservation, targets: List<String>, xValue: Int?, c: XConstraints): Boolean {
        if (!c.manaValue && !c.manaValueExactly && !c.power) return true
        if (xValue == null) return false
        val entities = entityFeatures(observation)
        for (target in targets) {
            val card = entities[target]
            if (c.manaValue && (card?.manaValue ?: (xValue + 1)) > xValue) return false
            if (c.manaValueExactly && card?.manaValue != xValue) return false
            if (c.power && card?.power != xValue) return false
        }
        return true
    }
}

/**
 * A policy's complete action parameters, as `inference.py` builds its params dict: a field is non-null exactly
 * when Python's dict carries the key (an empty `targets` or `attackers` is still present).
 */
data class Params(
    val convokePayments: Map<String, String?>? = null,
    val blightTarget: String? = null,
    val tappedPermanents: List<String>? = null,
    val xValue: Int? = null,
    val targets: List<String>? = null,
    val beheldCards: List<String>? = null,
    val attackers: Map<String, String>? = null,
    val blockers: Map<String, List<String>>? = null,
) {
    /** `json.dumps(params, sort_keys=True, separators=(",", ":"))`: the search's edge key and the parity form. */
    fun canonical(): String {
        val sb = StringBuilder("{")
        var first = true
        fun key(name: String) {
            if (!first) sb.append(',')
            first = false
            sb.append('"').append(name).append("\":")
        }
        fun str(s: String) = sb.append('"').append(s.replace("\\", "\\\\").replace("\"", "\\\"")).append('"')
        fun list(values: List<String>) {
            sb.append('[')
            values.forEachIndexed { i, v -> if (i > 0) sb.append(','); str(v) }
            sb.append(']')
        }
        attackers?.let { m ->
            key("attackers"); sb.append('{')
            m.keys.sorted().forEachIndexed { i, k -> if (i > 0) sb.append(','); str(k); sb.append(':'); str(m.getValue(k)) }
            sb.append('}')
        }
        beheldCards?.let { key("beheldCards"); list(it) }
        blightTarget?.let { key("blightTarget"); str(it) }
        blockers?.let { m ->
            key("blockers"); sb.append('{')
            m.keys.sorted().forEachIndexed { i, k -> if (i > 0) sb.append(','); str(k); sb.append(':'); list(m.getValue(k)) }
            sb.append('}')
        }
        convokePayments?.let { m ->
            key("convokePayments"); sb.append('{')
            m.keys.sorted().forEachIndexed { i, k ->
                if (i > 0) sb.append(',')
                str(k); sb.append(':')
                val color = m[k]
                if (color == null) sb.append("{}") else { sb.append("{\"color\":"); str(color); sb.append('}') }
            }
            sb.append('}')
        }
        tappedPermanents?.let { key("tappedPermanents"); list(it) }
        targets?.let { key("targets"); list(it) }
        xValue?.let { key("xValue"); sb.append(it) }
        return sb.append('}').toString()
    }

    fun toActionParams(): ActionParams = ActionParams(
        attackers = attackers.orEmpty().entries.associate { (k, v) -> EntityId(k) to EntityId(v) },
        blockers = blockers.orEmpty().entries.associate { (k, v) -> EntityId(k) to v.map(::EntityId) },
        targets = targets.orEmpty().map(::EntityId),
        xValue = xValue,
        convokePayments = convokePayments.orEmpty().entries.associate { (k, c) ->
            EntityId(k) to ConvokePayment(c?.let { Color.valueOf(it) })
        },
        blightTarget = blightTarget?.let(::EntityId),
        tappedPermanents = tappedPermanents.orEmpty().map(::EntityId),
        beheldCards = beheldCards.orEmpty().map(::EntityId),
    )
}
