package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mir.cards.Flash
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Flash (MIR #66) — "You may put a creature card from your hand onto the battlefield. If you do,
 * sacrifice it unless you pay its mana cost reduced by {2}."
 *
 * The payment itself (generic-only reduction, X as 0, enters triggers on a decline) is pinned by
 * `ManaCostOfPayCostTest`; here the card: Grizzly Bears ({1}{G}) stays for {G}, goes to the
 * graveyard on a decline, and choosing no creature does nothing at all.
 */
class FlashScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + Flash)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castFlash(caster: EntityId) {
        val flash = putCardInHand(caster, "Flash")
        giveMana(caster, Color.BLUE, 1)
        giveColorlessMana(caster, 1)
        castSpell(caster, flash).outcome shouldBe Outcome.Done
        giveMana(caster, Color.GREEN, 1)
        bothPass()
    }

    test("put Grizzly Bears in and keep it for {G}") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCardInHand(me, "Grizzly Bears")

        driver.castFlash(me)
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(me, listOf(bears))
        val pay = driver.pendingDecision as YesNoDecision
        pay.prompt shouldContain "Pay {G} or"
        driver.submitYesNo(me, true)

        driver.state.getBattlefield().contains(bears) shouldBe true
    }

    test("decline to pay and the creature is sacrificed") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCardInHand(me, "Grizzly Bears")

        driver.castFlash(me)
        driver.submitCardSelection(me, listOf(bears))
        driver.submitYesNo(me, false)

        driver.state.getZone(me, Zone.GRAVEYARD).contains(bears) shouldBe true
    }

    test("choosing no creature puts nothing in and asks for no payment") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCardInHand(me, "Grizzly Bears")

        driver.castFlash(me)
        driver.submitCardSelection(me, emptyList())

        withClue("no payment prompt follows an empty choice") { driver.isPaused shouldBe false }
        driver.state.getZone(me, Zone.HAND).contains(bears) shouldBe true
    }
})
