package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rites of Flourishing — "At the beginning of each player's draw step, that player draws an
 * additional card. Each player may play an additional land on each of their turns."
 *
 * The land clause is the symmetric `GrantAdditionalLandDrop(affected = Player.Each)`: the
 * enchantment's *opponent* gets the extra drop on their own turn, not just its controller.
 */
class RitesOfFlourishingScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("the controller may play two lands on their own turn, but not three") {
        val driver = newGame()
        val controller = driver.player1
        driver.putPermanentOnBattlefield(controller, "Rites of Flourishing")

        val lands = driver.getHand(controller).take(3)
        driver.playLand(controller, lands[0])
        driver.playLand(controller, lands[1])
        withClue("Two land drops: the normal one plus Rites'") {
            driver.submitExpectFailure(PlayLand(controller, lands[2]))
        }
    }

    test("the opponent draws an additional card on their draw step and may play two lands") {
        val driver = newGame()
        val controller = driver.player1
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(controller, "Rites of Flourishing")

        driver.passPriorityUntil(Step.DRAW)
        withClue("Now on the opponent's turn") { driver.activePlayer shouldBe opponent }
        val eventsBefore = driver.events.size
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val drawn = driver.events.drop(eventsBefore).filterIsInstance<CardsDrawnEvent>()
            .filter { it.playerId == opponent }.sumOf { it.count }
        withClue("The Rites trigger draws one card on top of the turn's normal draw") {
            drawn shouldBe 1
        }

        val lands = driver.getHand(opponent).take(3)
        driver.playLand(opponent, lands[0])
        driver.playLand(opponent, lands[1])
        withClue("The opponent gets the extra land drop too — 'each player'") {
            driver.submitExpectFailure(PlayLand(opponent, lands[2]))
        }
    }
})
