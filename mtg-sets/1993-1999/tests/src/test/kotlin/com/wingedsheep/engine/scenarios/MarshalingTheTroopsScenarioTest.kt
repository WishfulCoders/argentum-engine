package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class MarshalingTheTroopsScenarioTest : ScenarioTestBase() {
    private fun setup() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Marshaling the Troops")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLifeTotal(1, 20)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Marshaling the Troops") {
            test("taps chosen creatures and gains 4 life for each") {
                val game = setup()
                game.castSpell(1, "Marshaling the Troops").error shouldBe null
                game.resolveStack()
                val d = game.getPendingDecision() as SelectCardsDecision
                withClue("only my two untapped creatures are offered") { d.options.size shouldBe 2 }
                game.selectCards(d.options)
                game.getLifeTotal(1) shouldBe 28
                game.state.getEntity(d.options[0])!!.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
            }

            test("choosing none gains no life") {
                val game = setup()
                game.castSpell(1, "Marshaling the Troops").error shouldBe null
                game.resolveStack()
                game.selectCards(emptyList())
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
