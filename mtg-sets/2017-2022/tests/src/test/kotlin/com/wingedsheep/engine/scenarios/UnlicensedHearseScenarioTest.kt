package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.UnlicensedHearse
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unlicensed Hearse (SNC #246) — "{T}: Exile up to two target cards from a single graveyard.
 * Unlicensed Hearse's power and toughness are each equal to the number of cards exiled with it.
 * Crew 2"
 *
 * The exiled cards grow the Hearse (a 2/2 after one activation), the two targets must share a
 * graveyard, and crewed with nothing exiled it is a 0/0 that dies (ruling).
 */
class UnlicensedHearseScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + UnlicensedHearse)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
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

    fun GameTestDriver.exileWith(hearse: EntityId, vararg cards: ChosenTarget.Card) =
        submit(ActivateAbility(player1, hearse, UnlicensedHearse.script.activatedAbilities.first().id, targets = cards.toList()))

    test("two cards exiled from one graveyard make it a 2/2 once crewed") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val hearse = driver.putPermanentOnBattlefield(you, "Unlicensed Hearse")
        val a = driver.putCardInGraveyard(opponent, "Savannah Lions")
        val b = driver.putCardInGraveyard(opponent, "Centaur Courser")

        driver.exileWith(hearse, ChosenTarget.Card(a, opponent, Zone.GRAVEYARD), ChosenTarget.Card(b, opponent, Zone.GRAVEYARD))
            .error shouldBe null
        driver.settle()
        driver.getExileCardNames(opponent) shouldContain "Savannah Lions"
        driver.getExileCardNames(opponent) shouldContain "Centaur Courser"

        val crewA = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val crewB = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        driver.submitSuccess(CrewVehicle(you, hearse, listOf(crewA, crewB)))
        driver.settle()

        withClue("a creature now, with power and toughness equal to the two cards exiled with it") {
            driver.state.projectedState.isCreature(hearse) shouldBe true
            driver.state.projectedState.getPower(hearse) shouldBe 2
            driver.state.projectedState.getToughness(hearse) shouldBe 2
        }
    }

    test("the two targets must be in a single graveyard") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val hearse = driver.putPermanentOnBattlefield(you, "Unlicensed Hearse")
        val mine = driver.putCardInGraveyard(you, "Savannah Lions")
        val theirs = driver.putCardInGraveyard(opponent, "Savannah Lions")

        driver.exileWith(hearse, ChosenTarget.Card(mine, you, Zone.GRAVEYARD), ChosenTarget.Card(theirs, opponent, Zone.GRAVEYARD))
            .error shouldNotBe null
    }

    test("crewed with nothing exiled it is a 0/0 and dies") {
        val driver = newDriver()
        val you = driver.player1
        val hearse = driver.putPermanentOnBattlefield(you, "Unlicensed Hearse")
        val crewA = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val crewB = driver.putCreatureOnBattlefield(you, "Savannah Lions")

        driver.submitSuccess(CrewVehicle(you, hearse, listOf(crewA, crewB)))
        driver.settle()

        driver.getGraveyardCardNames(you) shouldContain "Unlicensed Hearse"
    }
})
