package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ghirapur Orrery — "Each player may play an additional land on each of their turns. At the
 * beginning of each player's upkeep, if that player has no cards in hand, that player draws three
 * cards."
 */
class GhirapurOrreryScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.emptyHand(player: EntityId) {
        var s = state
        for (card in getHand(player)) {
            s = s.moveToZone(card, ZoneKey(player, Zone.HAND), ZoneKey(player, Zone.GRAVEYARD))
        }
        replaceState(s)
    }

    test("an opponent with an empty hand at their upkeep draws three") {
        val driver = newGame()
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(driver.player1, "Ghirapur Orrery")
        driver.emptyHand(opponent)

        driver.passPriorityUntil(Step.DRAW)
        withClue("Three from the Orrery at upkeep, then the draw step's normal card") {
            driver.activePlayer shouldBe opponent
            driver.getHandSize(opponent) shouldBe 4
        }
    }

    test("a player with cards in hand at their upkeep draws nothing extra") {
        val driver = newGame()
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(driver.player1, "Ghirapur Orrery")
        val handBefore = driver.getHandSize(opponent)

        driver.passPriorityUntil(Step.DRAW)
        withClue("The intervening 'if' fails, so only the draw step's normal card arrives") {
            driver.getHandSize(opponent) shouldBe handBefore + 1
        }
    }

    test("the opponent may play an additional land on their turn") {
        val driver = newGame()
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(driver.player1, "Ghirapur Orrery")

        driver.passPriorityUntil(Step.DRAW)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        withClue("Now on the opponent's turn") { driver.activePlayer shouldBe opponent }

        val lands = driver.getHand(opponent).take(3)
        driver.playLand(opponent, lands[0])
        driver.playLand(opponent, lands[1])
        driver.submitExpectFailure(PlayLand(opponent, lands[2]))
    }
})
