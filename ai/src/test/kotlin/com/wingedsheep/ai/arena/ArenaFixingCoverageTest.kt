package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.buildSeededSealedDeck
import com.wingedsheep.ai.engine.draftableCards
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlin.random.Random

/**
 * What `just arena … <SET>` can and cannot measure — the guard on the arena's card universe.
 *
 * Written because a result was nearly misread. `just arena production production-fixing-crack 600
 * SOS` came back 300W-300L with a **zero-width** confidence interval — every pair split 1-1, which
 * is what happens when two profiles play identically, not when one is no better. The cause was
 * here: a set's `cards` are only the [com.wingedsheep.sdk.model.CardDefinition] values declared in
 * its own package, and a **reprint** declares a name-only [com.wingedsheep.sdk.model.Printing]
 * instead, so `set.cards` had no Terramorphic Expanse and the arm that re-prices a fetch land had
 * nothing to re-price.
 *
 * [draftableCards] fixes that by resolving each reprint to its canonical definition, the way
 * `GameBeansConfig.boosterCardPool` does for the real booster generator. These tests are the guard
 * that it stays fixed, and the general one is the first test: **no card the set prints may be
 * unopenable**. mtg-draft-ai `docs/48` is the census that found it; it also measures the stake —
 * the reprints are 26 % of the non-basic lands in a real SOS deck and 88 % in a real ECL deck.
 */
class ArenaFixingCoverageTest : FunSpec({

    context("every card a set prints can be opened") {
        // The general guard. A reprint is only skippable when its canonical definition is not
        // implemented anywhere, which for these two sets is nothing.
        listOf("SOS", "ECL").forEach { code ->
            test("$code: no printing is missing from the arena's pool") {
                val set = MtgSetCatalog.all.single { it.code == code }
                val openable = draftableCards(set).mapTo(hashSetOf()) { it.name }
                set.printings.map { it.name }.filterNot { it in openable }.shouldBeEmpty()
            }
        }
    }

    context("SOS mana fixing is reachable by an arena arm") {
        val set = MtgSetCatalog.all.single { it.code == "SOS" }
        val byName = draftableCards(set).associateBy { it.name }
        val decks = (0 until 300).map { buildSeededSealedDeck(set, Random(20260919L + it)) }

        fun countIn(predicate: (String) -> Boolean) = decks.map { deck -> deck.cards.count(predicate) }

        test("the sealed deck builder plays the set's fetch land") {
            // Was `shouldBe 0` before `draftableCards`: the `-crack` arm was inert, not null.
            countIn { it == "Terramorphic Expanse" }.count { it > 0 } shouldBeGreaterThan 0
        }

        test("it plays taplands in nearly every deck, so the land-drop arm has plenty to work on") {
            val taplands = countIn { name ->
                val card = byName[name]
                card != null && card.typeLine.isLand && name != "Terramorphic Expanse" &&
                    card.oracleText.contains("enters tapped")
            }
            taplands.count { it > 0 } shouldBeGreaterThan 280
            (taplands.sum().toDouble() / decks.size) shouldBeGreaterThan 3.0
        }
    }
})
