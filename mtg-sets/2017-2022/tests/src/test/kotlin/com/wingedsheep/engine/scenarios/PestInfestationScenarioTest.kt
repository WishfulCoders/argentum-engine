package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c21.cards.PestInfestation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Pest Infestation (C21 #65) — "Destroy up to X target artifacts and/or enchantments. Create twice X
 * 1/1 black and green Pest creature tokens with 'When this token dies, you gain 1 life.'"
 *
 * The X is both a target cap and a token count, and the two are independent (ruling): twice X
 * Pests however many targets were chosen, including none.
 */
class PestInfestationScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PestInfestation)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    fun GameTestDriver.pests(player: com.wingedsheep.sdk.model.EntityId) =
        getCreatures(player).count { getCardName(it)?.startsWith("Pest") == true }

    test("X = 2 with one target: the enchantment is destroyed and four Pests are made") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val enchantment = driver.putPermanentOnBattlefield(opponent, "Test Enchantment")
        val spell = driver.putCardInHand(you, "Pest Infestation")
        driver.giveMana(you, Color.GREEN, 5)

        driver.castXSpell(you, spell, xValue = 2, targets = listOf(enchantment)).error shouldBe null
        driver.settle()

        driver.getGraveyardCardNames(opponent) shouldContain "Test Enchantment"
        withClue("twice X, not twice the number of targets") {
            driver.pests(you) shouldBe 4
        }
    }

    test("X = 1 with no targets still makes two Pests, and a Pest's death gains 1 life") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val spell = driver.putCardInHand(you, "Pest Infestation")
        driver.giveMana(you, Color.GREEN, 3)

        driver.castXSpell(you, spell, xValue = 1).error shouldBe null
        driver.settle()
        driver.pests(you) shouldBe 2

        val pest = driver.getCreatures(you).first { driver.getCardName(it)?.startsWith("Pest") == true }
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.passPriority(you)
        driver.giveMana(opponent, Color.RED, 1)
        driver.castSpell(opponent, bolt, listOf(pest)).error shouldBe null
        driver.settle()

        driver.pests(you) shouldBe 1
        driver.getLifeTotal(you) shouldBe 21
    }
})
