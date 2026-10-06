package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.ExpressiveIteration
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Expressive Iteration — one of the top three to hand, one to the bottom, one to exile with a
 * this-turn play permission; and the two-card-library ruling (hand, then bottom, nothing exiled).
 */
class ExpressiveIterationScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ExpressiveIteration))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun cast(driver: GameTestDriver, player: EntityId) {
        val spell = driver.putCardInHand(player, "Expressive Iteration")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, spell).outcome shouldBe Outcome.Done
        driver.bothPass()
    }

    test("hand, bottom, and a playable exiled card") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val third = driver.putCardOnTopOfLibrary(player, "Centaur Courser")
        val second = driver.putCardOnTopOfLibrary(player, "Gurmag Angler")
        val first = driver.putCardOnTopOfLibrary(player, "Grizzly Bears")

        cast(driver, player)
        driver.submitCardSelection(player, listOf(second))   // to hand
        driver.submitCardSelection(player, listOf(third))    // to bottom; Grizzly Bears is exiled

        driver.getHand(player) shouldContain second
        val library = driver.state.getZone(ZoneKey(player, Zone.LIBRARY))
        library.last() shouldBe third
        library shouldNotContain first
        driver.state.getZone(ZoneKey(player, Zone.EXILE)) shouldContain first

        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)
        driver.legalActions(player).filter { (it.action as? CastSpell)?.cardId == first } shouldHaveSize 1
        driver.submit(CastSpell(playerId = player, cardId = first)).outcome shouldBe Outcome.Done
        driver.bothPass()
        (driver.findPermanent(player, "Grizzly Bears") != null) shouldBe true
    }

    test("with two cards in library, one goes to hand and one to the bottom; nothing is exiled") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val library = driver.state.getZone(ZoneKey(player, Zone.LIBRARY))
        library.forEach { driver.moveToGraveyard(it) }
        val second = driver.putCardOnTopOfLibrary(player, "Centaur Courser")
        val first = driver.putCardOnTopOfLibrary(player, "Grizzly Bears")

        cast(driver, player)
        driver.submitCardSelection(player, listOf(first))
        if (driver.isPaused) driver.submitCardSelection(player, listOf(second))

        driver.getHand(player) shouldContain first
        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)) shouldBe listOf(second)
        driver.state.getZone(ZoneKey(player, Zone.EXILE)) shouldHaveSize 0
    }
})
