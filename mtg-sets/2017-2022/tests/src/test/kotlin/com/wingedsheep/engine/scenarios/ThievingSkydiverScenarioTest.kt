package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.ThievingSkydiver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thieving Skydiver (ZNR #85) — "Kicker {X}. X can't be 0. … When this creature enters, if it was
 * kicked, gain control of target artifact with mana value X or less. If that artifact is an
 * Equipment, attach it to this creature."
 *
 * The X floor itself is pinned at the engine level by `SpellMinimumXTest`; here the card: the
 * kicked X bounds the target's mana value, an Equipment comes over attached, a non-Equipment
 * artifact just changes control, and an unkicked Skydiver triggers nothing.
 */
class ThievingSkydiverScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ThievingSkydiver)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Skydiver kicked with [x] from a floating pool and resolve it up to its trigger. */
    fun GameTestDriver.castKicked(me: EntityId, x: Int): EntityId {
        val diver = putCardInHand(me, "Thieving Skydiver")
        giveMana(me, Color.BLUE, 1)
        giveColorlessMana(me, 1 + x)
        submit(
            CastSpell(me, diver, xValue = x, declaredCostSlot = ChoiceSlot.KICKED, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        bothPass()
        return diver
    }

    test("kicked for 1: steals an Equipment of mana value 1 and attaches it; mana value 3 is out of reach") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bonesplitter = driver.putPermanentOnBattlefield(opponent, "Bonesplitter")
        val warhammer = driver.putPermanentOnBattlefield(opponent, "Loxodon Warhammer")

        val diver = driver.castKicked(me, x = 1)
        val decision = driver.pendingDecision as ChooseTargetsDecision
        withClue("only artifacts with mana value X = 1 or less are legal") {
            decision.legalTargets[0]!!.contains(bonesplitter) shouldBe true
            decision.legalTargets[0]!!.contains(warhammer) shouldBe false
        }
        driver.submitTargetSelection(me, listOf(bonesplitter))
        driver.bothPass()

        driver.state.projectedState.getController(bonesplitter) shouldBe me
        driver.state.getEntity(bonesplitter)?.get<AttachedToComponent>()?.targetId shouldBe diver
        driver.state.projectedState.getController(warhammer) shouldBe opponent
    }

    test("a non-Equipment artifact changes control and is not attached") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val solRing = driver.putPermanentOnBattlefield(opponent, "Sol Ring")

        driver.castKicked(me, x = 1)
        driver.submitTargetSelection(me, listOf(solRing))
        driver.bothPass()

        driver.state.projectedState.getController(solRing) shouldBe me
        driver.state.getEntity(solRing)?.get<AttachedToComponent>() shouldBe null
    }

    test("unkicked, it triggers nothing") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val solRing = driver.putPermanentOnBattlefield(opponent, "Sol Ring")
        val diver = driver.putCardInHand(me, "Thieving Skydiver")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)

        driver.castSpell(me, diver).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.isPaused shouldBe false
        driver.stackSize shouldBe 0
        driver.state.projectedState.getController(solRing) shouldBe opponent
    }

    test("kicked with X = 0 is refused") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val diver = driver.putCardInHand(me, "Thieving Skydiver")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)

        driver.submitExpectFailure(
            CastSpell(me, diver, xValue = 0, declaredCostSlot = ChoiceSlot.KICKED, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldNotBe null
    }
})
