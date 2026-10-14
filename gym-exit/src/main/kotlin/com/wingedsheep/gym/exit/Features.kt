package com.wingedsheep.gym.exit

import com.wingedsheep.gym.contract.EntityFeatures
import com.wingedsheep.gym.contract.LegalActionView
import com.wingedsheep.gym.contract.PlayerView
import com.wingedsheep.gym.contract.StackItemView
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.sdk.core.Zone
import java.util.zip.CRC32

/**
 * Port of mtg-draft-ai `src/mtgdraft/gameplay/features.py` (`gameplay-features@v1`) to the in-JVM
 * [TrainingObservation] (docs/65 §3 item 2).
 *
 * The Python featurizer reads the gym's JSON, whose encoder omits null fields (`explicitNulls = false`), so
 * `card.get("controllerId", card.get("ownerId"))` falls back to the owner exactly when the controller is null.
 * Every such read is mirrored here; the parity test (`FeatureParityTest`) holds the two to exact equality.
 */
object FeatureSpec {
    const val VERSION = "gameplay-features@v1"
    const val MAX_ENTITIES = 128
    const val MAX_ACTIONS = 96
    const val MAX_X = 15
    const val DESCRIPTION_BUCKETS = 4096

    val STEPS = listOf(
        "UNTAP", "UPKEEP", "DRAW", "PRECOMBAT_MAIN", "BEGIN_COMBAT", "DECLARE_ATTACKERS",
        "DECLARE_BLOCKERS", "FIRST_STRIKE_COMBAT_DAMAGE", "COMBAT_DAMAGE", "END_COMBAT",
        "POSTCOMBAT_MAIN", "END", "CLEANUP",
    )
    val PENDING_KINDS = listOf(
        "SELECT_CARDS", "YES_NO", "CHOOSE_OPTION", "CHOOSE_COLOR", "CHOOSE_TARGETS",
        "COMBAT_RESOLUTION", "CHOOSE_NUMBER", "ORDER_OBJECTS",
    )
    val ACTION_KINDS = listOf(
        "PassPriority", "PlayLand", "CastSpell", "ActivateAbility", "DeclareAttackers",
        "DeclareBlockers", "DECISION", "CrewVehicle", "CastWithKicker", "CycleCard", "TypecycleCard",
        "CastSpellMode", "CastWithFlashback", "CastWithWarp", "SaddleMount", "CastFaceDown",
        "CastSpellModal", "PlotCard", "CastWithHarmonize", "UnlockRoomDoor", "CastWithAlternativeCost",
        "CastWithDash", "CastWithoutPayingManaCost",
    )
    val ZONES = listOf("Stack", "Battlefield", "Hand", "Graveyard", "Exile", "Library", "Command", "Sideboard")
    val STACK_KINDS = listOf("SPELL", "TRIGGERED_ABILITY", "ACTIVATED_ABILITY")
    val CARD_TYPES = listOf(
        "LAND", "BASIC", "CREATURE", "ARTIFACT", "INSTANT", "SORCERY", "ENCHANTMENT", "LEGENDARY",
        "KINDRED", "PLANESWALKER", "BATTLE",
    )
    val COLORS = listOf("WHITE", "BLUE", "BLACK", "RED", "GREEN")
    val KEYWORDS = listOf(
        "FLYING", "VIGILANCE", "REACH", "TRAMPLE", "WARD", "MENACE", "DEATHTOUCH", "HASTE", "FLASH",
        "LIFELINK", "DEFENDER", "FIRST_STRIKE", "DOUBLE_STRIKE", "PROWESS", "INDESTRUCTIBLE",
        "HEXPROOF", "CANT_BE_BLOCKED", "CHANGELING", "DOESNT_UNTAP", "CREW", "SADDLE", "FLASHBACK",
        "CONVOKE", "AFFINITY",
    )
    val KEYWORD_SET = KEYWORDS.toSet()
    val COUNTERS = listOf("PLUS_ONE_PLUS_ONE", "MINUS_ONE_MINUS_ONE", "LORE", "STUN")
    val MANA = listOf("W", "U", "B", "R", "G", "C")
    val ZONE_CAPS = mapOf("Graveyard" to 16, "Exile" to 8)

    val GLOBAL_WIDTH = 1 + STEPS.size + 1 + 4 + PENDING_KINDS.size + 1 + 1
    val PLAYER_WIDTH = 5 + MANA.size + 4
    val ENTITY_WIDTH = 4 + ZONES.size + 1 + STACK_KINDS.size + 1 + CARD_TYPES.size + COLORS.size + KEYWORDS.size +
        1 + 9 + 10 + COUNTERS.size + 1 + 6
    val ACTION_WIDTH = ACTION_KINDS.size + 1 + 9 + 9

    /** The column of an action row that says the action is affordable: the action pointer's hard mask. */
    val AFFORDABLE_COLUMN = ACTION_KINDS.size + 1
}

/** The JSON name of a zone (`@SerialName`), which is what the Python featurizer compares against. */
val Zone.wire: String
    get() = when (this) {
        Zone.LIBRARY -> "Library"
        Zone.HAND -> "Hand"
        Zone.BATTLEFIELD -> "Battlefield"
        Zone.GRAVEYARD -> "Graveyard"
        Zone.STACK -> "Stack"
        Zone.EXILE -> "Exile"
        Zone.COMMAND -> "Command"
        Zone.SIDEBOARD -> "Sideboard"
    }

/** One visible object: a zone card ([card]) or a stack item ([stack]), as `_visible_cards` lists them. */
class Visible(val card: EntityFeatures?, val stack: StackItemView?, val zone: String, val stackPosition: Int) {
    val entityId: String get() = card?.entityId?.value ?: stack!!.entityId.value
    val name: String get() = card?.name ?: stack!!.name

    /** `card_name`: the definition id before `#`, else the name. */
    val cardName: String
        get() {
            val definition = card?.cardDefinitionId?.takeIf { it.isNotEmpty() } ?: name
            return definition.substringBefore('#')
        }
}

class Features(
    val glob: IntArray,
    val players: Array<IntArray>,
    val entities: Array<IntArray>,
    val cards: List<String>,
    val actions: Array<IntArray>,
    val actionSource: IntArray,
    val actionDescription: IntArray,
    val actionIds: IntArray,
    val pointerIds: List<String>,
    val visible: List<Visible>,
) {
    val entityCount: Int get() = cards.size
    val actionCount: Int get() = actionIds.size
}

object Featurizer {
    private val SYMBOL = Regex("\\{([^}]+)\\}")
    // (?U): Python 3's \\w, \\s and \\b are Unicode-aware; Java's are ASCII unless asked.
    private val DESCRIPTION_NAME = Regex(
        "(?U)^(?:Cast|Play|Activate|Cycle|Crew|Saddle|Plot|Unlock)\\w*\\s+(.+?)(?:\\s+\\(.*\\))?$",
    )
    private val THIS = Regex("(?U)\\bthis (?:permanent|creature|artifact|enchantment|land|card|vehicle)\\b")

    fun clip(value: Int?, high: Int = 255): Int = maxOf(0, minOf(value ?: 0, high))

    private fun oneHot(out: IntArray, at: Int, value: String?, vocabulary: List<String>): Int {
        if (value != null) {
            val i = vocabulary.indexOf(value)
            out[at + (if (i >= 0) i else vocabulary.size)] = 1
        }
        return at + vocabulary.size + 1
    }

    /** Generic, W U B R G C, hybrid and X symbol counts of a mana cost string. */
    fun manaCostFeatures(cost: String?): IntArray {
        var generic = 0
        var x = 0
        var hybrid = 0
        val colored = IntArray(6)
        for (match in SYMBOL.findAll(cost ?: "")) {
            val symbol = match.groupValues[1]
            when {
                symbol.isNotEmpty() && symbol.all { it.isDigit() } -> generic += symbol.toInt()
                symbol == "X" -> x++
                symbol in FeatureSpec.MANA -> colored[FeatureSpec.MANA.indexOf(symbol)]++
                else -> hybrid++
            }
        }
        return intArrayOf(clip(generic), *colored, clip(hybrid), clip(x))
    }

    fun descriptionBucket(description: String): Int {
        val crc = CRC32()
        crc.update(description.toByteArray(Charsets.UTF_8))
        return 1 + (crc.value % (FeatureSpec.DESCRIPTION_BUCKETS - 1)).toInt()
    }

    private fun global(observation: TrainingObservation, me: String): IntArray {
        val out = IntArray(FeatureSpec.GLOBAL_WIDTH)
        val pending = observation.pendingDecision
        var at = 0
        out[at++] = clip(observation.turnNumber)
        at = oneHot(out, at, observation.step.name, FeatureSpec.STEPS)
        out[at++] = if (observation.activePlayerId?.value == me) 1 else 0
        out[at++] = if (observation.priorityPlayerId?.value == me) 1 else 0
        out[at++] = clip(observation.stack.size)
        out[at++] = if (observation.terminated) 1 else 0
        at = oneHot(out, at, pending?.kind?.name, FeatureSpec.PENDING_KINDS)
        out[at] = if (pending != null) 1 else 0
        return out
    }

    private fun playerRow(player: PlayerView): IntArray {
        val pool = player.manaPool
        return intArrayOf(
            player.lifeTotal,
            clip(player.handSize), clip(player.librarySize), clip(player.graveyardSize), clip(player.exileSize),
            clip(pool.white), clip(pool.blue), clip(pool.black), clip(pool.red), clip(pool.green), clip(pool.colorless),
            if (player.isActive) 1 else 0,
            if (player.hasPriority) 1 else 0,
            if (player.hasLost) 1 else 0,
            if (player.isPerspective) 1 else 0,
        )
    }

    private fun entityRow(item: Visible, me: String, playerIds: Set<String>): IntArray {
        val out = IntArray(FeatureSpec.ENTITY_WIDTH)
        val card = item.card
        val stack = item.stack
        val controller = card?.controllerId?.value ?: card?.ownerId?.value ?: stack?.controllerId?.value
        val owner = card?.ownerId?.value
        var at = 0
        out[at++] = if (controller == me) 1 else 0
        out[at++] = if (owner == me) 1 else 0
        out[at++] = if (card?.tapped == true) 1 else 0
        out[at++] = if (card?.summoningSick == true) 1 else 0
        at = oneHot(out, at, item.zone, FeatureSpec.ZONES)
        at = oneHot(out, at, if (item.zone == "Stack") stack?.kind?.name else null, FeatureSpec.STACK_KINDS)
        val types = card?.types.orEmpty()
        for (t in FeatureSpec.CARD_TYPES) out[at++] = if (t in types) 1 else 0
        val colors = card?.colors.orEmpty()
        for (c in FeatureSpec.COLORS) out[at++] = if (c in colors) 1 else 0
        val keywords = card?.keywords.orEmpty()
        for (k in FeatureSpec.KEYWORDS) out[at++] = if (k in keywords) 1 else 0
        out[at++] = if (keywords.any { it !in FeatureSpec.KEYWORD_SET }) 1 else 0
        for (v in manaCostFeatures(card?.manaCost ?: "")) out[at++] = v
        out[at++] = clip(card?.manaValue)
        out[at++] = if (card?.power != null) 1 else 0
        out[at++] = clip(card?.power)
        out[at++] = clip(card?.toughness)
        out[at++] = clip(card?.damageMarked)
        out[at++] = if (card?.faceDown == true) 1 else 0
        out[at++] = if (card?.attachedTo != null) 1 else 0
        out[at++] = clip(card?.attachments?.size)
        out[at++] = clip(item.stackPosition)
        out[at++] = clip(stack?.targets?.size)
        val counters = card?.counters.orEmpty()
        for (c in FeatureSpec.COUNTERS) out[at++] = clip(counters[c])
        out[at++] = clip(counters.entries.filter { it.key !in FeatureSpec.COUNTERS }.sumOf { it.value })
        out[at++] = if (card?.attacking == true) 1 else 0
        val attackTarget = card?.attackTargetId?.value
        out[at++] = if (attackTarget == me) 1 else 0
        out[at++] = if (attackTarget != null && attackTarget in playerIds && attackTarget != me) 1 else 0
        out[at++] = if (attackTarget != null && attackTarget !in playerIds) 1 else 0
        out[at++] = clip(card?.blockingEntityIds?.size)
        out[at] = clip(card?.blockedByEntityIds?.size)
        return out
    }

    fun visibleCards(observation: TrainingObservation, me: String): List<Visible> {
        val byZone = HashMap<String, MutableList<EntityFeatures>>()
        for (zone in observation.zones) {
            val kind = zone.zoneType.wire
            if (kind == "Hand" && zone.ownerId.value != me) continue
            byZone.getOrPut(kind) { mutableListOf() }.addAll(zone.cards)
        }
        val out = ArrayList<Visible>()
        val stack = observation.stack
        for ((position, item) in stack.withIndex()) out += Visible(null, item, "Stack", stack.size - position)
        for (kind in listOf("Battlefield", "Hand", "Graveyard", "Exile", "Command")) {
            var cards: List<EntityFeatures> = byZone[kind].orEmpty()
            val cap = FeatureSpec.ZONE_CAPS[kind]
            if (cap != null) cards = cards.takeLast(cap)
            for (card in cards) out += Visible(card, null, kind, 0)
        }
        return if (out.size > FeatureSpec.MAX_ENTITIES) out.subList(0, FeatureSpec.MAX_ENTITIES) else out
    }

    private fun abilityText(text: String, name: String): String {
        var t = text.lowercase()
        if (name.isNotEmpty()) t = t.replace(name.lowercase(), "this")
        return THIS.replace(t, "this").trimEnd('.')
    }

    /** A v1.5 action's source, recovered from its description; -1 when it cannot be (see features.py). */
    private fun sourceFromDescription(
        action: LegalActionView, cards: List<Visible>, byName: Map<String, Int>, me: String,
    ): Int {
        val description = action.description
        val match = DESCRIPTION_NAME.matchEntire(description)
        if (match != null) byName[match.groupValues[1]]?.let { return it }
        if (action.kind != "ActivateAbility" || description.isEmpty()) return -1
        val wanted = abilityText(description, "")
        for (key in listOf(wanted, wanted.substringBefore('.'), wanted.substringBefore(':') + ":")) {
            var found = -1
            for ((i, item) in cards.withIndex()) {
                val card = item.card ?: continue
                if (item.zone != "Battlefield" || card.controllerId?.value != me) continue
                if (key in abilityText(card.oracleText, card.name)) {
                    if (!card.tapped) return i
                    if (found < 0) found = i
                }
            }
            if (found >= 0) return found
        }
        return -1
    }

    private fun actionRow(action: LegalActionView, optionIndex: Int): IntArray {
        val out = IntArray(FeatureSpec.ACTION_WIDTH)
        var at = oneHot(out, 0, action.kind, FeatureSpec.ACTION_KINDS)
        out[at++] = if (action.affordable) 1 else 0
        out[at++] = if (action.targetEntityIds.isNotEmpty() || action.targetRequirements.isNotEmpty()) 1 else 0
        out[at++] = clip(action.targetEntityIds.size)
        out[at++] = clip(action.targetRequirements.size)
        out[at++] = if (action.hasXCost) 1 else 0
        out[at++] = clip(action.maxAffordableX)
        out[at++] = if (action.isManaAbility) 1 else 0
        out[at++] = if (action.isDecisionOption) 1 else 0
        out[at++] = clip(optionIndex)
        for (v in manaCostFeatures(action.manaCost ?: "")) out[at++] = v
        return out
    }

    fun featurize(observation: TrainingObservation): Features {
        val me = observation.perspectivePlayerId.value
        val players = observation.players.sortedBy { it.id.value != me }
        val cards = visibleCards(observation, me)
        val entityIds = cards.map { it.entityId }
        val position = HashMap<String, Int>(entityIds.size * 2)
        for ((i, id) in entityIds.withIndex()) position[id] = i
        val byName = HashMap<String, Int>()
        for ((i, item) in cards.withIndex()) {
            if (item.zone != "Stack") byName.putIfAbsent(item.card?.name ?: "", i)
        }
        val legal = observation.legalActions.let { if (it.size > FeatureSpec.MAX_ACTIONS) it.subList(0, FeatureSpec.MAX_ACTIONS) else it }
        val rows = ArrayList<IntArray>(legal.size)
        val sources = IntArray(legal.size)
        val descriptions = IntArray(legal.size)
        val actionIds = IntArray(legal.size)
        var decisionIndex = 0
        for ((i, action) in legal.withIndex()) {
            rows += actionRow(action, if (action.isDecisionOption) decisionIndex else 0)
            if (action.isDecisionOption) decisionIndex++
            val source = action.sourceEntityId
            sources[i] = if (source != null) position[source.value] ?: -1 else sourceFromDescription(action, cards, byName, me)
            val kind = action.kind
            descriptions[i] = if (kind == "DECISION" || kind == "ActivateAbility" || kind == "CastSpellMode") {
                descriptionBucket(action.description)
            } else 0
            actionIds[i] = action.actionId
        }
        val playerIds = players.map { it.id.value }.toSet()
        return Features(
            glob = global(observation, me),
            players = players.map { playerRow(it) }.toTypedArray(),
            entities = cards.map { entityRow(it, me, playerIds) }.toTypedArray(),
            cards = cards.map { it.cardName },
            actions = rows.toTypedArray(),
            actionSource = sources,
            actionDescription = descriptions,
            actionIds = actionIds,
            pointerIds = entityIds + players.map { it.id.value },
            visible = cards,
        )
    }
}
