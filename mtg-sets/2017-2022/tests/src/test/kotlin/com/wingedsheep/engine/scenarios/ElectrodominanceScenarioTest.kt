package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rna.cards.Electrodominance
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Electrodominance (RNA #99) — "Electrodominance deals X damage to any target. You may cast a spell
 * with mana value X or less from your hand without paying its mana cost."
 *
 * X caps both the damage and the free spell: with X = 2 a one-drop is offered and cast for nothing,
 * a three-drop is not offered.
 */
class ElectrodominanceScenarioTest : FunSpec({

    test("X = 2: 2 damage, and a spell of mana value 2 or less cast from hand for free") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + Electrodominance)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val lions = driver.putCardInHand(you, "Savannah Lions")
        val courser = driver.putCardInHand(you, "Centaur Courser")
        val spell = driver.putCardInHand(you, "Electrodominance")
        driver.giveMana(you, Color.RED, 4)

        driver.castXSpell(you, spell, xValue = 2, targets = listOf(opponent)).error shouldBe null
        var offered: List<com.wingedsheep.sdk.model.EntityId>? = null
        var guard = 0
        while (guard++ < 30) {
            val d = driver.pendingDecision
            when {
                d is SelectCardsDecision -> { offered = d.options; driver.submitCardSelection(you, listOf(lions)) }
                d != null -> driver.autoResolveDecision()
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }

        driver.getLifeTotal(opponent) shouldBe 18
        withClue("the three-drop is over X and not offered") {
            offered!! shouldContain lions
            offered!! shouldNotContain courser
        }
        withClue("the Lions were cast without mana and resolved") {
            driver.getCreatures(you) shouldContain lions
            driver.getHand(you) shouldContain courser
        }
    }
})
