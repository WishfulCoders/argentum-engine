package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.LurrusOfTheDreamDen
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lurrus of the Dream-Den — "Once during each of your turns, you may cast a permanent spell with
 * mana value 2 or less from your graveyard."
 *
 * Covers the filter (permanent, mana value <= 2 — not a 3-drop, not an instant, never a land) and
 * the once-per-turn allowance.
 */
class LurrusOfTheDreamDenScenarioTest : FunSpec({

    val twoDrop = card("Test Two-Drop Relic") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }
    val otherTwoDrop = card("Test Two-Drop Bear") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Bear"
        power = 2
        toughness = 2
    }
    val threeDrop = card("Test Three-Drop Relic") {
        manaCost = "{3}"
        typeLine = "Artifact"
    }
    val cheapInstant = card("Test Cheap Instant") {
        manaCost = "{1}"
        typeLine = "Instant"
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LurrusOfTheDreamDen, twoDrop, otherTwoDrop, threeDrop, cheapInstant))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun graveyardLandPlays(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.legalActions(player).filter { (it.action as? PlayLand)?.cardId == cardId }

    fun graveyardCasts(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.legalActions(player)
            .filter { it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == cardId }

    test("offers permanent spells with mana value 2 or less, and nothing else") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Lurrus of the Dream-Den")

        val relic = driver.putCardInGraveyard(player, "Test Two-Drop Relic")
        val big = driver.putCardInGraveyard(player, "Test Three-Drop Relic")
        val instant = driver.putCardInGraveyard(player, "Test Cheap Instant")
        val land = driver.putCardInGraveyard(player, "Plains")
        driver.giveColorlessMana(player, 3)

        graveyardCasts(driver, player, relic) shouldHaveSize 1
        withClue("mana value 3 is over the cap") { graveyardCasts(driver, player, big) shouldHaveSize 0 }
        withClue("an instant is not a permanent spell") { graveyardCasts(driver, player, instant) shouldHaveSize 0 }
        withClue("Lurrus doesn't let you play lands from your graveyard") {
            graveyardLandPlays(driver, player, land) shouldHaveSize 0
        }
    }

    test("only one graveyard cast per turn") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Lurrus of the Dream-Den")

        val relic = driver.putCardInGraveyard(player, "Test Two-Drop Relic")
        val bear = driver.putCardInGraveyard(player, "Test Two-Drop Bear")
        driver.giveColorlessMana(player, 4)

        driver.submit(CastSpell(playerId = player, cardId = relic)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(player, "Test Two-Drop Relic") shouldNotBe null

        withClue("the once-per-turn allowance is spent") {
            graveyardCasts(driver, player, bear) shouldHaveSize 0
        }
    }

    test("does nothing on the opponent's turn") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.putPermanentOnBattlefield(opponent, "Lurrus of the Dream-Den")
        val relic = driver.putCardInGraveyard(opponent, "Test Two-Drop Relic")
        driver.giveColorlessMana(opponent, 2)
        graveyardCasts(driver, opponent, relic) shouldHaveSize 0
    }
})
