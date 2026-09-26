package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Plaguecrafter — "When this creature enters, each player sacrifices a creature or planeswalker of
 * their choice. Each player who can't discards a card."
 *
 * The load-bearing case: "who can't" is decided before the sacrifices. A player whose only
 * creature is sacrificed *could* sacrifice, so they don't also discard.
 */
class PlaguecrafterScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castPlaguecrafterAndResolve() {
        val crafter = putCardInHand(player1, "Plaguecrafter")
        giveMana(player1, Color.BLACK, 3)
        castSpell(player1, crafter)
        var guard = 0
        while ((stackSize > 0 || pendingDecision != null) && guard++ < 20) {
            if (pendingDecision != null) {
                val choice = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                submitCardSelection(choice.playerId, choice.options.take(choice.minSelections.coerceAtLeast(1)))
            } else {
                passPriority(state.priorityPlayerId!!)
            }
        }
    }

    test("a player without creatures discards; the controller sacrifices Plaguecrafter and keeps their hand") {
        val driver = newGame()
        val controllerHand = driver.getHandSize(driver.player1)
        val opponentHand = driver.getHandSize(driver.player2)

        driver.castPlaguecrafterAndResolve()

        withClue("Plaguecrafter was its controller's only creature, so it was sacrificed") {
            driver.getGraveyardCardNames(driver.player1) shouldBe listOf("Plaguecrafter")
        }
        withClue("…and its controller could sacrifice, so they did not discard") {
            driver.getHandSize(driver.player1) shouldBe controllerHand
        }
        withClue("The opponent controlled no creature or planeswalker, so they discarded one card") {
            driver.getHandSize(driver.player2) shouldBe opponentHand - 1
        }
    }

    test("a player with a creature sacrifices it instead of discarding") {
        val driver = newGame()
        driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val opponentHand = driver.getHandSize(driver.player2)

        driver.castPlaguecrafterAndResolve()

        withClue("The opponent sacrificed their Bears and kept their hand") {
            driver.getGraveyardCardNames(driver.player2) shouldBe listOf("Grizzly Bears")
            driver.getHandSize(driver.player2) shouldBe opponentHand
        }
    }
})
