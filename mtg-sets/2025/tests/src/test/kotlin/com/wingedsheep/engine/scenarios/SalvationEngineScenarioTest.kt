package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dft.cards.SalvationEngine
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Salvation Engine. */
class SalvationEngineScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SalvationEngine)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        return driver
    }

    test("pumps other artifact creatures you control, not creatures that aren't artifacts") {
        val driver = createDriver()
        val active = driver.activePlayer!!

        driver.putPermanentOnBattlefield(active, "Salvation Engine")
        val artifactCreature = driver.putCreatureOnBattlefield(active, "Artifact Creature")
        val courser = driver.putCreatureOnBattlefield(active, "Centaur Courser")

        driver.state.projectedState.getPower(artifactCreature) shouldBe 4
        driver.state.projectedState.getToughness(artifactCreature) shouldBe 4

        driver.state.projectedState.getPower(courser) shouldBe 3
        driver.state.projectedState.getToughness(courser) shouldBe 3
    }
})
