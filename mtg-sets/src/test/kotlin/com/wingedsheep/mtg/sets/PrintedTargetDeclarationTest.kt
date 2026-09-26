package com.wingedsheep.mtg.sets

import com.wingedsheep.sdk.tooling.CardExporter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Corpus-wide gate: a printed "target" is a real target.
 *
 * "Return another **target** creature card" and "return another creature card" resolve to the same
 * board when nothing goes wrong, which is why the difference is easy to script away — a
 * gather → select → move pipeline reads like the card. It is still a rules divergence: nothing is
 * chosen when the ability is put on the stack (CR 601.2c / 603.3d), so hexproof, shroud, ward and
 * protection are never checked, and the ability can't be countered for want of a legal target
 * (CR 608.2b). This test fails when a card's Oracle text names a target but its compiled script
 * declares no target requirement.
 *
 * Two populations, both paired without guessing:
 *  - **The card.** Any sentence that names a target, while nothing anywhere in the script targets.
 *  - **A modal spell's modes.** When the printed bullets and the script's modes line up one-to-one,
 *    a bullet that names a target must be a mode that declares one.
 *
 * The sentence match drops reminder text and quoted granted abilities (a token's "…deals 1 damage
 * to any target" is that token's text, not this card's), and skips phrases where "target" is not a
 * target this object chooses: "becomes the target of", "can't be the target", "choose new
 * targets", "a spell that targets …".
 */
class PrintedTargetDeclarationTest : FunSpec({

    val reminder = Regex("""\([^()]*\)""")
    val quoted = Regex("""["“][^"”]*["”]""")
    val sentence = Regex("""\n+|(?<=[.])\s+(?=[A-Z•])""")
    val namesTarget = Regex(
        """\bany target\b|\btarget (?!of\b)(?:[a-z-]+ ){0,5}?[a-z-]+\b""",
        RegexOption.IGNORE_CASE
    )
    val notOwnTarget = Regex(
        """becomes? the target|be the target|can't be targeted|new targets?|change the targets?|""" +
            """reselect|single target|one or more targets|targets? remain|""" +
            """\b(?:that|it|which|could|would) targets?\b|targets only|equip abilit|""" +
            """less to activate|each target|target of""",
        RegexOption.IGNORE_CASE
    )
    // Serial names of every TargetRequirement subtype, plus the keyword abilities whose target is
    // implied by the keyword itself (equip "target creature you control", …).
    val declaresTarget = Regex(
        """"(?:AnyTarget|TargetCreatureOrPlaneswalker|TargetCreatureOrPlayer|TargetObject|""" +
            """TargetOpponent|TargetOpponentOrPlaneswalker|TargetOther|TargetPermanentOrPlayer|""" +
            """TargetPlayer|TargetPlayerOrPlaneswalker|TargetSpellOrPermanent|""" +
            """Equip|Ninjutsu|Reconfigure|Bestow|Crew)""""
    )

    fun printedTargets(text: String): List<String> =
        quoted.replace(reminder.replace(text, ""), "")
            .split(sentence)
            .map { it.trim() }
            .filter { namesTarget.containsMatchIn(it) && !notOwnTarget.containsMatchIn(it) }

    fun modeLists(element: JsonElement, into: MutableList<JsonArray>) {
        when (element) {
            is JsonObject -> {
                (element["modes"] as? JsonArray)?.takeIf { modes -> modes.all { it is JsonObject } }
                    ?.let { into += it }
                element.values.forEach { modeLists(it, into) }
            }
            is JsonArray -> element.forEach { modeLists(it, into) }
            else -> {}
        }
    }

    // Cards that print a target the engine can't yet declare, each with the missing vocabulary.
    // A card leaves this list the moment it can be scripted with a real target requirement.
    val allowlist = mapOf(
        // "Sacrifice a creature you control with mana value X": X is announced with the activation
        // (CR 107.3a) and bounds the target, but the only sacrifice cost that yields an X before
        // targets are chosen (`CostAtom.VariablePermanents`) is "one or more", with no exactly-one form.
        "Sidisi, Regent of the Mire" to "needs an exactly-one sacrifice cost that announces X",
        // Two "up to N" target groups in one spell. Cast-time targets are a flat, fixed-width
        // positional list, so an under-filled optional group that isn't the last one shifts every
        // later target into the wrong requirement.
        "Rite of Renewal" to "needs per-requirement target grouping for two optional multi-target slots",
    )

    val cards = MtgSetCatalog.all.flatMap { set -> set.cards.map { set.code to it } }
        .filter { (_, card) -> card.name !in allowlist }
    var printing = 0
    var printingModes = 0

    val offenders = cards.flatMap { (code, card) ->
        val oracle = card.oracleText.orEmpty()
        val printed = printedTargets(oracle)
        if (printed.isEmpty()) return@flatMap emptyList()
        printing++
        val exported = CardExporter.exportToJson(card)
        if (!declaresTarget.containsMatchIn(exported)) {
            return@flatMap listOf("[$code] ${card.name}: prints a target but declares none — \"${printed.first()}\"")
        }
        val bullets = reminder.replace(oracle, "").lines()
            .map { it.trim() }
            .filter { it.startsWith("•") }
            .map { it.removePrefix("•").trim() }
        val modes = mutableListOf<JsonArray>().also { modeLists(Json.parseToJsonElement(exported), it) }
            .singleOrNull()
            ?.takeIf { it.size == bullets.size }
            ?: return@flatMap emptyList()
        bullets.zip(modes).mapNotNull { (bullet, mode) ->
            if (printedTargets(bullet).isEmpty()) return@mapNotNull null
            printingModes++
            if (declaresTarget.containsMatchIn(mode.toString())) null
            else "[$code] ${card.name}: mode \"$bullet\" prints a target but declares none"
        }
    }

    test("a card whose Oracle text names a target declares a target requirement") {
        if (offenders.isNotEmpty()) {
            println("=== printed targets with no target requirement: ${offenders.size} ===")
            offenders.forEach { println("  $it") }
        }
        offenders.shouldBeEmpty()
    }

    // The gate can only fail on sentences it recognises. A floor keeps a regex tweak from turning it
    // green by matching nothing.
    test("every allowlisted card still prints a target it doesn't declare") {
        val stale = allowlist.keys.filter { name ->
            val card = MtgSetCatalog.all.flatMap { it.cards }.firstOrNull { it.name == name }
                ?: return@filter true
            printedTargets(card.oracleText.orEmpty()).isEmpty() ||
                declaresTarget.containsMatchIn(CardExporter.exportToJson(card))
        }
        stale.shouldBeEmpty()
    }

    test("the gate covers the cards it is supposed to cover") {
        println("cards printing a target: $printing, modes printing a target: $printingModes")
        check(printing >= 4000) { "only $printing cards matched; the target regex has stopped working" }
        check(printingModes >= 400) { "only $printingModes modes matched; the mode pairing has stopped working" }
    }
})
