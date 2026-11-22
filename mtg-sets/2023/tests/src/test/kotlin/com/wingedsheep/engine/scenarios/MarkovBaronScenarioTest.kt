package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Markov Baron (MAT #14) — {2}{B} 2/2 Vampire Noble. Convoke, lifelink, "Other Vampires you
 * control get +1/+1", madness {2}{B}.
 */
class MarkovBaronScenarioTest : FunSpec({

    val testVampire = CardDefinition.creature("Test Vampire", ManaCost.parse("{1}{B}"), setOf(Subtype.VAMPIRE), 2, 2)
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + testVampire)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("other Vampires you control get +1/+1, the Baron and opposing Vampires do not") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val baron = driver.putCreatureOnBattlefield(player, "Markov Baron")
        val ally = driver.putCreatureOnBattlefield(player, "Test Vampire")
        val enemy = driver.putCreatureOnBattlefield(opponent, "Test Vampire")

        val projected = projector.project(driver.state)
        projected.getPower(ally) shouldBe 3
        projected.getToughness(ally) shouldBe 3
        projected.getPower(baron) shouldBe 2
        projected.getPower(enemy) shouldBe 2
    }

    test("discarded, it is exiled and can be cast for its madness cost of {2}{B}") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val baron = driver.putCardInHand(player, "Markov Baron")
        val voice = driver.putCardInHand(player, "Tormenting Voice")
        driver.giveMana(player, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(baron)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.getExile(player) shouldContain baron

        driver.giveMana(player, Color.BLACK, 3)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
        driver.submitYesNo(player, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(player, "Markov Baron") shouldBe baron
    }
})
