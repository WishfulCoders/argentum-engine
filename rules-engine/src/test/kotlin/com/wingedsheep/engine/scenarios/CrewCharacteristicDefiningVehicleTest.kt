package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Crewing a Vehicle whose power and toughness are characteristic-defining (a printed `*`, CR 604.3).
 *
 * Crew makes the Vehicle an artifact creature (CR 702.122a) and says nothing about its power and
 * toughness, so a `*`/`*` Vehicle keeps its defining value — Unlicensed Hearse is as big as the pile
 * it has exiled. The crew handler used to stamp the missing fixed value, 0, so every such Vehicle
 * was a 0/0 the moment it was crewed and died to state-based actions.
 */
class CrewCharacteristicDefiningVehicleTest : FunSpec({

    val handVehicle = card("Test Hand-Sized Vehicle") {
        manaCost = "{2}"
        typeLine = "Artifact — Vehicle"
        oracleText = "This Vehicle's power and toughness are each equal to the number of cards in your hand.\nCrew 1"
        dynamicStats(DynamicAmount.Count(Player.You, Zone.HAND))
        keywordAbility(KeywordAbility.crew(1))
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 20) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    test("a crewed */* Vehicle keeps its characteristic-defining power and toughness, live") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + handVehicle)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1
        val vehicle = driver.putPermanentOnBattlefield(you, "Test Hand-Sized Vehicle")
        val crew = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val hand = driver.getHand(you).size

        driver.submitSuccess(CrewVehicle(you, vehicle, listOf(crew)))
        driver.settle()

        withClue("a creature, as big as the hand") {
            driver.state.projectedState.isCreature(vehicle) shouldBe true
            driver.state.projectedState.getPower(vehicle) shouldBe hand
            driver.state.projectedState.getToughness(vehicle) shouldBe hand
        }

        driver.putCardInHand(you, "Plains")
        withClue("still defined by the hand after the crew effect has resolved") {
            driver.state.projectedState.getPower(vehicle) shouldBe hand + 1
        }
    }
})
