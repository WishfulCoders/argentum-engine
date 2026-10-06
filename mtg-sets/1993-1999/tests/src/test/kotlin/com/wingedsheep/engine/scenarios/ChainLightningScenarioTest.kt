package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.ChainLightning
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Chain Lightning (LEG #137): "Chain Lightning deals 3 damage to any target. Then that player or that
 * permanent's controller may pay {R}{R}. If the player does, they may copy this spell and may choose
 * a new target for that copy."
 *
 * Exercises the chain copy's **mana** cost: the affected player (here, the controller of the
 * damaged creature) pays {R}{R} with their own lands while Chain Lightning resolves.
 */
class ChainLightningScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ChainLightning))
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, me, opp)
    }

    fun GameTestDriver.bolt(caster: EntityId, target: EntityId) {
        val chain = putCardInHand(caster, "Chain Lightning")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, chain, listOf(target)).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("the damaged creature's controller pays {R}{R} and chains it back at the caster") {
        val (driver, me, opp) = setup()
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val m1 = driver.putLandOnBattlefield(opp, "Mountain")
        val m2 = driver.putLandOnBattlefield(opp, "Mountain")

        driver.bolt(me, bears)

        val offer = driver.pendingDecision
        offer.shouldBeInstanceOf<YesNoDecision>()
        offer.playerId shouldBe opp
        offer.yesText shouldBe "Pay {R}{R}"
        driver.submitYesNo(opp, true)

        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        driver.submitManaAutoPayOrDecline(opp, autoPay = true)
        driver.isTapped(m1) shouldBe true
        driver.isTapped(m2) shouldBe true

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(opp, listOf(me))
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 17
        // State-based actions run once the chain has finished resolving.
        driver.assertInGraveyard(opp, "Grizzly Bears")
        // I have no red sources left, so the chain ends without asking me.
        driver.isPaused shouldBe false
        driver.stackSize shouldBe 0
    }

    test("declining to pay ends the chain") {
        val (driver, me, opp) = setup()
        driver.putLandOnBattlefield(opp, "Mountain")
        driver.putLandOnBattlefield(opp, "Mountain")

        driver.bolt(me, opp)
        driver.getLifeTotal(opp) shouldBe 17
        driver.submitYesNo(opp, false)

        driver.isPaused shouldBe false
        driver.stackSize shouldBe 0
        driver.getLifeTotal(me) shouldBe 20
    }

    test("a player who can't produce {R}{R} isn't offered the copy") {
        val (driver, me, opp) = setup()
        driver.putLandOnBattlefield(opp, "Mountain")

        driver.bolt(me, opp)

        driver.getLifeTotal(opp) shouldBe 17
        driver.isPaused shouldBe false
        driver.stackSize shouldBe 0
    }
})
