package com.wingedsheep.sdk.scripting.filters.unified

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.util.pluralNounPhrase

/**
 * The object noun phrase a [TargetFilter] names, in the word order Oracle text prints it:
 * adjectives, then the type noun, then qualifiers, then where the object is and who controls it.
 *
 * ```
 * Creature.youControl()                        -> "creature you control"
 * Creature.tapped()                            -> "tapped creature"
 * Artifact or Creature.tapped()                -> "artifact or tapped creature"
 * Creature @ GRAVEYARD, owned by you           -> "creature card in your graveyard"
 * Noncreature @ STACK                          -> "noncreature spell"
 * Permanent.withSubtype(VAMPIRE).youControl()  -> "Vampire you control"
 * ```
 *
 * This is the text of the targeting prompt the client shows, so it is rendered from the filter's
 * structure rather than by concatenating each predicate's own [GameObjectFilter.description]
 * (which puts the controller first: "you control creature").
 */
internal object TargetPhrase {

    fun describe(filter: TargetFilter, plural: Boolean = false): String =
        filter.clauses().joinToString(" or ") { clause(it, plural) }

    private fun clause(filter: TargetFilter, plural: Boolean): String {
        val base = filter.baseFilter
        val (head, qualifiers) = head(base, filter.zone, plural)
        val location = location(filter.zone, base.controllerPredicate ?: branchController(base))
        // "creature you control with power 2 or less", but "creature card with mana value 3 or
        // less in your graveyard": a controller reads right after the noun, a zone after everything
        val parts = if (filter.zone == Zone.BATTLEFIELD || filter.zone == Zone.STACK) {
            listOf(head, location, qualifiers)
        } else {
            listOf(head, qualifiers, location)
        }
        return parts.filter { it.isNotEmpty() }.joinToString(" ")
    }

    /** A controller shared by every `anyOf` branch reads once, after the whole group. */
    private fun branchController(filter: GameObjectFilter): ControllerPredicate? =
        filter.anyOf.map { it.controllerPredicate }.distinct().singleOrNull()

    /** The head noun group and, separately, its trailing qualifiers. */
    private fun head(filter: GameObjectFilter, zone: Zone, plural: Boolean): Pair<String, String> {
        val own = words(filter)
        val branches = flatten(filter)
        if (branches.isEmpty()) return nounGroup(own, zone, plural) to own.qualifiers.joinToString(" ")
        val shared = branchController(filter)
        val branchWords = branches.map(::words)
        // "red or white creature": branches that only add adjectives modify the base noun
        if (shared == null && branches.all { it.controllerPredicate == null } &&
            branchWords.all { it.nouns.isEmpty() && it.qualifiers.isEmpty() && it.adjectives.isNotEmpty() }
        ) {
            val merged = Words(own.adjectives + oxfordOr(branchWords.map { it.adjectives.joinToString(" ") }), own.nouns, own.qualifiers)
            return nounGroup(merged, zone, plural) to own.qualifiers.joinToString(" ")
        }
        val rendered = branches.map { branch ->
            val w = words(branch)
            val group = nounGroup(w, zone, plural)
            val controller = if (shared == null) controllerSuffix(branch.controllerPredicate) else ""
            listOf(group, controller, w.qualifiers.joinToString(" ")).filter { it.isNotEmpty() }.joinToString(" ")
        }
        // base predicates apply to every branch: "nonland artifact or creature"
        val prefix = own.adjectives.joinToString(" ")
        return listOf(prefix, oxfordOr(rendered)).filter { it.isNotEmpty() }.joinToString(" ") to
            own.qualifiers.joinToString(" ")
    }

    /** `(A or B) or C` is one three-way choice. */
    private fun flatten(filter: GameObjectFilter): List<GameObjectFilter> = filter.anyOf.flatMap { branch ->
        val bare = branch.cardPredicates.isEmpty() && branch.statePredicates.isEmpty() && branch.controllerPredicate == null
        if (bare && branch.anyOf.isNotEmpty()) flatten(branch) else listOf(branch)
    }

    private class Words(
        val adjectives: List<String>,
        val nouns: List<String>,
        val qualifiers: List<String>,
    )

    private fun words(filter: GameObjectFilter): Words {
        val adjectives = mutableListOf<String>()
        val modifiers = mutableListOf<String>()
        val nouns = mutableListOf<String>()
        val qualifiers = mutableListOf<String>()
        filter.statePredicates.forEach { p ->
            val d = stateText(p)
            when {
                d.isEmpty() -> {}
                ' ' !in d || isAdjectiveGroup(d) -> adjectives += d
                else -> qualifiers += d
            }
        }
        filter.cardPredicates.forEach { p ->
            val d = p.description
            when {
                p is CardPredicate.HasSubtype -> modifiers += d
                p is CardPredicate.HasAnyOfSubtypes -> modifiers += oxfordOr(p.subtypes.map { it.value })
                p is CardPredicate.Or && members(p).all { it is CardPredicate.HasSubtype } ->
                    modifiers += oxfordOr(members(p).map { it.description })
                // "artifact, enchantment, or creature with flying": each alternative is its own phrase
                p is CardPredicate.Or -> {
                    val inner = members(p).map { member ->
                        words(GameObjectFilter(cardPredicates = (member as? CardPredicate.And)?.predicates ?: listOf(member)))
                    }
                    when {
                        // "red or white": alternatives that are all adjectives modify the noun
                        inner.all { it.nouns.isEmpty() && it.qualifiers.isEmpty() } ->
                            adjectives += oxfordOr(inner.map { it.adjectives.joinToString(" ") })
                        // "with power 2 or less or with toughness 2 or less"
                        inner.all { it.nouns.isEmpty() } ->
                            qualifiers += oxfordOr(inner.map { (it.adjectives + it.qualifiers).joinToString(" ") })
                        else -> nouns += oxfordOr(inner.map {
                            (it.adjectives + it.nouns.ifEmpty { listOf("permanent") } + it.qualifiers).joinToString(" ")
                        })
                    }
                }
                p is CardPredicate.Not && p.predicate.description in NOUNS -> adjectives += "non" + p.predicate.description.substringBefore(' ')
                isNoun(d) -> nouns += d
                d.startsWith("has ") || d.startsWith("one or more") -> qualifiers += "that ${if (d.startsWith("has ")) d else "has $d"}"
                QUALIFIER_START.any { d.startsWith(it) } -> qualifiers += d
                else -> adjectives += d
            }
        }
        // "Vampire permanent" is printed "Vampire", "Equipment artifact" "Equipment": a subtype names
        // the object on its own. Creature types keep the noun ("Goblin creature"), as Oracle does.
        if (modifiers.isNotEmpty() && nouns.size == 1 && nouns[0] in SUBTYPE_IMPLIES) nouns.clear()
        // "basic land" + "land" reads once
        if ("basic land" in nouns) nouns.remove("land")
        // "nonland permanent" + "artifact or Saga" reads "nonland artifact or Saga"
        if ("permanent" in nouns && nouns.size > 1) nouns.remove("permanent")
        if ("token" in nouns && nouns.size > 1) {
            nouns.remove("token"); nouns += "token"
        }
        return Words(adjectives, modifiers + nouns, qualifiers)
    }

    /** `(A or B) or C` is one three-way choice. */
    private fun members(p: CardPredicate.Or): List<CardPredicate> =
        p.predicates.flatMap { if (it is CardPredicate.Or) members(it) else listOf(it) }

    private fun stateText(p: StatePredicate): String {
        val d = p.description
        return when {
            d.startsWith("put into ") -> "that was $d"
            VERB_START.any { d.startsWith(it) } -> "that $d"
            else -> d
        }
    }

    private fun isAdjectiveGroup(d: String): Boolean =
        d.split(" or ", " and ").all { ' ' !in it.trim() }

    private fun isNoun(d: String): Boolean =
        d in NOUNS || d.split(" or ").all { it.trim() in NOUNS || it.trim().firstOrNull()?.isUpperCase() == true }

    private fun nounGroup(w: Words, zone: Zone, plural: Boolean): String {
        var noun = w.nouns.joinToString(" ").ifEmpty { "" }
        noun = when (zone) {
            Zone.STACK -> when {
                noun.isEmpty() -> "spell"
                noun.endsWith("ability") -> noun
                noun.contains("ability") -> noun // "instant or sorcery or activated or triggered ability"
                else -> "$noun spell"
            }
            Zone.BATTLEFIELD -> noun.ifEmpty { "permanent" }
            else -> when {
                noun.isEmpty() -> "card"
                noun.endsWith("card") -> noun
                else -> "$noun card"
            }
        }
        if (plural) noun = pluralNounPhrase(noun)
        return (w.adjectives + noun).filter { it.isNotEmpty() }.joinToString(" ")
    }

    private fun location(zone: Zone, controller: ControllerPredicate?): String = when (zone) {
        Zone.BATTLEFIELD, Zone.STACK -> controllerSuffix(controller)
        Zone.GRAVEYARD -> "in ${possessive(controller)} graveyard"
        Zone.HAND -> "in ${possessive(controller)} hand"
        Zone.LIBRARY -> "in ${possessive(controller)} library"
        Zone.EXILE -> listOf("in exile", ownerSuffix(controller)).filter { it.isNotEmpty() }.joinToString(" ")
        else -> "in ${zone.displayName}"
    }

    private fun controllerSuffix(controller: ControllerPredicate?): String = when {
        controller is ControllerPredicate.Not && controller.predicate == ControllerPredicate.ControlledByYou -> "you don't control"
        else -> controller?.description.orEmpty()
    }

    private fun ownerSuffix(controller: ControllerPredicate?): String = when (controller) {
        ControllerPredicate.OwnedByYou -> "you own"
        ControllerPredicate.OwnedByOpponent -> "an opponent owns"
        else -> ""
    }

    private fun possessive(controller: ControllerPredicate?): String = when (controller) {
        ControllerPredicate.OwnedByYou, ControllerPredicate.ControlledByYou -> "your"
        ControllerPredicate.OwnedByOpponent, ControllerPredicate.ControlledByOpponent -> "an opponent's"
        else -> "a"
    }

    private fun oxfordOr(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        2 -> "${items[0]} or ${items[1]}"
        else -> items.dropLast(1).joinToString(", ") + ", or " + items.last()
    }

    private val SUBTYPE_IMPLIES = setOf("permanent", "artifact", "enchantment", "land", "planeswalker", "battle")

    private val NOUNS = setOf(
        "creature", "land", "artifact", "enchantment", "planeswalker", "instant", "sorcery", "permanent",
        "battle", "basic land", "double-faced card", "token", "card", "spell",
        "activated or triggered ability", "triggered ability", "activated ability",
    )

    private val QUALIFIER_START = listOf(
        "with ", "without ", "that ", "of ", "named ", "originally ", "whose ", "in ", "on ",
        "attached ", "exiled ", "created ", "cast ", "from ",
    )

    private val VERB_START = listOf(
        "entered ", "became ", "was ", "has ", "dealt ", "attacked ", "couldn't ", "blocked this", "put into ",
        "crewed ",
    )
}
