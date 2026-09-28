package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Toxic Deluge (C13 #96) — {2}{B} Sorcery.
 *
 * "As an additional cost to cast this spell, pay X life.
 *  All creatures get -X/-X until end of turn."
 *
 * The X paid as life through [com.wingedsheep.sdk.scripting.AdditionalCost.PayXLife] must reach the
 * -X/-X group effect at resolution, for both players' creatures.
 */
class ToxicDelugeScenarioTest : FunSpec({

    fun GameTestDriver.resolveStack() {
        var safety = 0
        while (stackSize > 0 && !isPaused && safety < 20) {
            bothPass(); safety++
        }
    }

    test("pay 2 life: every creature gets -2/-2, so X/2s die and bigger creatures shrink") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2 — dies
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")   // 2/2 — dies (all creatures)
        val giant = driver.putCreatureOnBattlefield(opponent, "Hill Giant") // 3/3 — survives as 1/1

        repeat(3) { driver.putLandOnBattlefield(player, "Swamp") }

        val deluge = driver.putCardInHand(player, "Toxic Deluge")
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = deluge,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = 2)
            )
        )
        driver.resolveStack()

        driver.getLifeTotal(player) shouldBe 18
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.findPermanent(player, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponent, "Hill Giant") shouldBe giant
        driver.state.projectedState.getPower(giant) shouldBe 1
        driver.state.projectedState.getToughness(giant) shouldBe 1
    }

    test("pay 0 life: nothing changes") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        repeat(3) { driver.putLandOnBattlefield(player, "Swamp") }

        val deluge = driver.putCardInHand(player, "Toxic Deluge")
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = deluge,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = 0)
            )
        )
        driver.resolveStack()

        driver.getLifeTotal(player) shouldBe 20
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe bears
        driver.state.projectedState.getPower(bears) shouldBe 2
    }
})
