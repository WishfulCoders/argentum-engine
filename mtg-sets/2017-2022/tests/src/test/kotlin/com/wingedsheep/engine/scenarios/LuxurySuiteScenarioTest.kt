package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bbd.cards.LuxurySuite
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Luxury Suite — "This land enters tapped unless you have two or more opponents."
 *
 * The battlebond lands are the first cards to read `PlayerCount(EachOpponent)` inside an
 * `EntersTapped` unless-condition, so this pins the count against real two- and three-player
 * games, including the ruling that it is the opponents you have *now*, not at the start.
 */
class LuxurySuiteScenarioTest : FunSpec({

    val deck = Deck.of("Swamp" to 40)

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(LuxurySuite))
    }

    test("with one opponent it enters tapped") {
        val driver = driver()
        driver.initMirrorMatch(deck = deck, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val suite = driver.putCardInHand(me, "Luxury Suite")
        driver.playLand(me, suite).error shouldBe null

        driver.isTapped(suite) shouldBe true
    }

    test("with two opponents it enters untapped") {
        val driver = driver()
        val players = driver.initMultiplayer(decks = List(3) { deck }, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        me shouldBe players[0]

        val suite = driver.putCardInHand(me, "Luxury Suite")
        driver.playLand(me, suite).error shouldBe null

        driver.isTapped(suite) shouldBe false
    }

    test("a three-player game down to one opponent counts the opponents you have now") {
        val driver = driver()
        val players = driver.initMultiplayer(decks = List(3) { deck }, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        driver.concede(players[2]).error shouldBe null

        val suite = driver.putCardInHand(me, "Luxury Suite")
        driver.playLand(me, suite).error shouldBe null

        driver.isTapped(suite) shouldBe true
    }
})
