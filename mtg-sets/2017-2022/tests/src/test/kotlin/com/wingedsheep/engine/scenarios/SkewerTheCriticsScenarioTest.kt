package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rna.cards.SkewerTheCritics
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Skewer the Critics (RNA #115) — "Spectacle {R}. Skewer the Critics deals 3 damage to any target."
 *
 * Spectacle is composed rather than new vocabulary (a `SelfAlternativeCost` gated on
 * `Conditions.OpponentLostLifeThisTurn`), so this pins the gate from the card's side: the {R}
 * cast is refused until an opponent has lost life this turn, and taken afterwards.
 */
class SkewerTheCriticsScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SkewerTheCritics)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    fun spectacleCast(you: EntityId, skewer: EntityId, target: EntityId) = CastSpell(
        you, skewer,
        targets = listOf(ChosenTarget.Player(target)),
        useAlternativeCost = true,
        paymentStrategy = PaymentStrategy.FromPool,
    )

    test("the spectacle cost is refused while no opponent has lost life this turn") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val skewer = driver.putCardInHand(you, "Skewer the Critics")
        driver.giveMana(you, Color.RED, 1)

        driver.submit(spectacleCast(you, skewer, opponent)).error shouldNotBe null
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("after an opponent loses life, it can be cast for {R} and deals 3 damage") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(opponent)).error shouldBe null
        driver.settle()
        driver.getLifeTotal(opponent) shouldBe 17

        val skewer = driver.putCardInHand(you, "Skewer the Critics")
        driver.giveMana(you, Color.RED, 1)
        driver.submit(spectacleCast(you, skewer, opponent)).error shouldBe null
        driver.settle()

        withClue("one red mana paid the whole cost, and the spell resolved") {
            driver.getLifeTotal(opponent) shouldBe 14
            driver.getGraveyardCardNames(you).contains("Skewer the Critics") shouldBe true
        }
    }
})
