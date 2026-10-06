package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Timetwister
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Timetwister {2}{U} — Sorcery.
 *
 * "Each player shuffles their hand and graveyard into their library, then draws seven cards.
 *  (Then put Timetwister into its owner's graveyard.)"
 *
 * The wheel touches both players' hands, graveyards and libraries, so the test pins the zone sizes
 * on each side and that Timetwister itself lands in the graveyard afterwards rather than being
 * shuffled in (2013-07-01 ruling).
 */
class TimetwisterScenarioTest : FunSpec({

    test("each player shuffles hand and graveyard into library, then draws seven") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Timetwister))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.putCardInGraveyard(you, "Grizzly Bears")
        driver.putCardInGraveyard(you, "Lightning Bolt")
        driver.putCardInGraveyard(opponent, "Grizzly Bears")

        fun library(p: com.wingedsheep.sdk.model.EntityId) = driver.state.getZone(ZoneKey(p, Zone.LIBRARY)).size
        val yourTotal = library(you) + driver.getHandSize(you) + driver.getGraveyard(you).size
        val oppTotal = library(opponent) + driver.getHandSize(opponent) + driver.getGraveyard(opponent).size

        val twister = driver.putCardInHand(you, "Timetwister")
        driver.giveMana(you, Color.BLUE, 3)
        driver.castSpell(you, twister).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.getHandSize(you) shouldBe 7
        driver.getHandSize(opponent) shouldBe 7
        driver.getGraveyardCardNames(you) shouldBe listOf("Timetwister")
        driver.getGraveyardCardNames(opponent) shouldBe emptyList()
        // Every card that was in hand/graveyard went back into the library before the draw.
        library(you) shouldBe yourTotal - 7
        library(opponent) shouldBe oppTotal - 7
    }
})
