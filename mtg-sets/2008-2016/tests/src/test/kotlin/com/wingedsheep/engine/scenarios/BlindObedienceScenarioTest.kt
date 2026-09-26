package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.BlindObedience
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Blind Obedience (GTC #6): extort drains 1 when you cast a spell and pay {W/B}; creatures your
 * opponents control enter tapped, yours don't.
 */
class BlindObedienceScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BlindObedience)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(driver.player1, "Blind Obedience")
        return driver
    }

    fun GameTestDriver.settle(pay: Boolean) {
        var guard = 0
        while (guard++ < 30) {
            val d = pendingDecision
            when {
                d is YesNoDecision -> submitYesNo(d.playerId, pay)
                d is SelectManaSourcesDecision -> autoResolveDecision()
                d != null -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    test("extort: casting a spell and paying {W/B} drains each opponent for 1") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val lions = driver.putCardInHand(you, "Savannah Lions")
        driver.giveMana(you, Color.WHITE, 2)

        driver.castSpell(you, lions).error shouldBe null
        driver.settle(pay = true)

        driver.getLifeTotal(opponent) shouldBe 19
        driver.getLifeTotal(you) shouldBe 21
        driver.isTapped(driver.findPermanent(you, "Savannah Lions")!!) shouldBe false
    }

    test("a creature an opponent casts enters tapped") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.activePlayerId shouldBe opponent

        val lions = driver.putCardInHand(opponent, "Savannah Lions")
        driver.giveMana(opponent, Color.WHITE, 1)
        driver.castSpell(opponent, lions).error shouldBe null
        driver.settle(pay = false)

        driver.isTapped(driver.findPermanent(opponent, "Savannah Lions")!!) shouldBe true
    }
})
