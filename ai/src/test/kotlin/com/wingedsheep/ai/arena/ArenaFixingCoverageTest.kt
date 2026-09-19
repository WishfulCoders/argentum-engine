package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.buildSeededSealedDeck
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * What `just arena … SOS` can and cannot measure about mana fixing.
 *
 * Written because a result was nearly misread. `just arena production production-fixing-crack 600
 * SOS` came back 300W-300L with a **zero-width** confidence interval — every pair split 1-1, which
 * is what happens when two profiles play identically, not when one is no better. The reason is
 * here: [buildSeededSealedDeck] never puts a Terramorphic Expanse in a deck, so the arm that
 * changes how a fetch land is priced has nothing to change.
 *
 * The consequence for mtg-draft-ai `docs/46`: the local arena measures the **land-drop** half of
 * the fix on decks full of taplands, and cannot measure the **fetch** half at all. Only a run on
 * real 17Lands builds can — 28 % of them play the card. A future reader comparing the `-crack`,
 * `-choose` and `-colour` arms needs to know that before reading the first one as a null.
 */
class ArenaFixingCoverageTest : FunSpec({

    val set = MtgSetCatalog.all.single { it.code == "SOS" }
    val byName = set.cards.associateBy { it.name }
    val decks = (0 until 300).map { buildSeededSealedDeck(set.cards, Random(20260919L + it)) }

    fun countIn(predicate: (String) -> Boolean) = decks.map { deck -> deck.cards.count(predicate) }

    test("the sealed deck builder never plays the set's fetch land, so the crack arm is inert") {
        countIn { it == "Terramorphic Expanse" }.sum() shouldBe 0
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
})
