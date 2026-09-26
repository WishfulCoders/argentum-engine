package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tempt with Bunnies — tempting offer: you draw a card and make a 1/1 Rabbit; each opponent may do
 * the same; for each opponent who does, you do it again.
 *
 * Proves `Patterns.Mechanic.temptingOffer` against the cycle's ruling in a three-player game:
 * opponents answer in turn order, *nobody* acts until every opponent has answered, then each
 * accepter gets the offer and the caster gets it once more per acceptance.
 */
class TemptWithBunniesScenarioTest : FunSpec({

    data class Pod(val driver: GameTestDriver, val caster: EntityId, val next: EntityId, val last: EntityId)

    fun newPod(): Pod {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        val players = driver.initMultiplayer(decks = List(3) { Deck.of("Plains" to 40) }, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Pod(driver, players[0], players[1], players[2])
    }

    fun GameTestDriver.rabbits(player: EntityId): Int =
        getPermanents(player).count { getCardName(it) == "Rabbit Token" }

    /** Cast Tempt with Bunnies for [caster] and pass priority around until it starts resolving. */
    fun GameTestDriver.castTempt(caster: EntityId) {
        val tempt = putCardInHand(caster, "Tempt with Bunnies")
        giveMana(caster, Color.WHITE, 3)
        castSpell(caster, tempt)
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 10) {
            passPriority(state.priorityPlayerId!!)
        }
    }

    test("one opponent accepts, one declines: nothing happens for anyone until both have answered") {
        val (driver, caster, next, last) = newPod()
        val hands = listOf(caster, next, last).associateWith { driver.getHandSize(it) }
        driver.castTempt(caster)

        withClue("The caster has taken the offer before anyone is asked") {
            driver.rabbits(caster) shouldBe 1
            driver.getHandSize(caster) shouldBe hands.getValue(caster) + 1
        }
        val first = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        withClue("The next opponent in turn order is asked first") { first.playerId shouldBe next }
        driver.submitYesNo(next, true)

        val second = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        withClue("Then the last opponent — and the accepter has not drawn or made a Rabbit yet") {
            second.playerId shouldBe last
            driver.rabbits(next) shouldBe 0
            driver.getHandSize(next) shouldBe hands.getValue(next)
        }
        driver.submitYesNo(last, false)

        withClue("The accepting opponent drew a card and made a Rabbit") {
            driver.rabbits(next) shouldBe 1
            driver.getHandSize(next) shouldBe hands.getValue(next) + 1
        }
        withClue("The declining opponent got nothing") {
            driver.rabbits(last) shouldBe 0
            driver.getHandSize(last) shouldBe hands.getValue(last)
        }
        withClue("The caster took the offer once more for the one acceptance") {
            driver.rabbits(caster) shouldBe 2
            driver.getHandSize(caster) shouldBe hands.getValue(caster) + 2
        }
    }

    test("both opponents accept: the caster takes the offer three times in all") {
        val (driver, caster, next, last) = newPod()
        driver.castTempt(caster)
        driver.submitYesNo(next, true)
        driver.submitYesNo(last, true)

        driver.rabbits(next) shouldBe 1
        driver.rabbits(last) shouldBe 1
        driver.rabbits(caster) shouldBe 3
    }

    test("every opponent declines: the caster takes the offer exactly once") {
        val (driver, caster, next, last) = newPod()
        driver.castTempt(caster)
        driver.submitYesNo(next, false)
        driver.submitYesNo(last, false)

        withClue("No more decisions, and only the caster's own Rabbit") {
            driver.pendingDecision shouldBe null
            driver.rabbits(caster) shouldBe 1
            driver.rabbits(next) shouldBe 0
            driver.rabbits(last) shouldBe 0
        }
    }
})
