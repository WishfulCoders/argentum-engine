package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.WheelOfFortune
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Wheel of Fortune — "Each player discards their hand, then draws seven cards."
 *
 * Every player's old hand ends up in their graveyard, nobody discards a freshly drawn card, and
 * every player — including one with an empty hand — ends with exactly seven.
 */
class WheelOfFortuneScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WheelOfFortune))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("each player discards their hand, then draws seven") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val wheel = driver.putCardInHand(player, "Wheel of Fortune")
        val myOldHand = driver.getHand(player).filter { it != wheel }
        val theirOldHand = driver.getHand(opponent)
        theirOldHand.size shouldBe 7

        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2)
        driver.castSpell(player, wheel).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getHandSize(player) shouldBe 7
        driver.getHandSize(opponent) shouldBe 7
        driver.getGraveyard(player) shouldContainAll myOldHand + wheel
        driver.getGraveyard(player) shouldHaveSize myOldHand.size + 1
        driver.getGraveyard(opponent) shouldContainAll theirOldHand
        driver.getGraveyard(opponent) shouldHaveSize theirOldHand.size
        driver.getHand(opponent).forEach { theirOldHand shouldNotContain it }
    }

    test("a player with an empty hand still draws seven") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val wheel = driver.putCardInHand(player, "Wheel of Fortune")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2)
        driver.castSpell(player, wheel).outcome shouldBe Outcome.Done
        // Wheel is on the stack; the caster's hand is now the old seven. Empty it first.
        driver.getHand(player).forEach { driver.moveToGraveyard(it) }
        driver.getHandSize(player) shouldBe 0
        driver.bothPass()
        driver.getHandSize(player) shouldBe 7
    }
})
