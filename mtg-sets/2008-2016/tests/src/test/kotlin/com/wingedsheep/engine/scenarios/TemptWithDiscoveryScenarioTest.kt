package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tempt with Discovery — tempting offer over "search your library for a land card and put it onto
 * the battlefield". The accepting opponent searches *their own* library (the offer runs with them
 * as "you"), and the caster searches once more for the acceptance.
 */
class TemptWithDiscoveryScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.forests(player: EntityId): Int =
        getPermanents(player).count { getCardName(it) == "Forest" }

    /** Answer the pending library search for [player] by taking the first offered land. */
    fun GameTestDriver.searchFirst(player: EntityId) {
        val search = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.playerId shouldBe player
        submitCardSelection(player, listOf(search.options.first()))
    }

    fun GameTestDriver.castTempt(caster: EntityId) {
        val tempt = putCardInHand(caster, "Tempt with Discovery")
        giveMana(caster, Color.GREEN, 4)
        castSpell(caster, tempt)
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 10) {
            passPriority(state.priorityPlayerId!!)
        }
    }

    test("the opponent accepts: each searches their own library, and the caster searches twice") {
        val driver = newGame()
        val caster = driver.player1
        val opponent = driver.player2
        val opponentLibrary = driver.state.getLibrary(opponent).size
        driver.castTempt(caster)

        driver.searchFirst(caster)
        withClue("The caster's first land is in play before the opponent is asked") {
            driver.forests(caster) shouldBe 1
        }
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opponent
        driver.submitYesNo(opponent, true)

        driver.searchFirst(opponent)
        withClue("The opponent put a land from their own library onto their own battlefield") {
            driver.forests(opponent) shouldBe 1
            driver.state.getLibrary(opponent).size shouldBe opponentLibrary - 1
        }

        driver.searchFirst(caster)
        withClue("The caster searched again for the acceptance") {
            driver.forests(caster) shouldBe 2
            driver.pendingDecision shouldBe null
        }
    }

    test("the opponent declines: the caster searches once") {
        val driver = newGame()
        val caster = driver.player1
        val opponent = driver.player2
        driver.castTempt(caster)

        driver.searchFirst(caster)
        driver.submitYesNo(opponent, false)

        withClue("No further searches") {
            driver.pendingDecision shouldBe null
            driver.forests(caster) shouldBe 1
            driver.forests(opponent) shouldBe 0
        }
    }
})
