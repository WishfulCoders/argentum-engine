package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class ControlOfTheCourtScenarioTest : ScenarioTestBase() {
    init {
        context("Control of the Court") {
            test("draws four then discards three at random, netting one card") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Control of the Court")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lib = game.librarySize(1)
                game.castSpell(1, "Control of the Court").error shouldBe null
                game.resolveStack()

                game.librarySize(1) shouldBe lib - 4
                game.handSize(1) shouldBe 1
                // spell itself + three random discards
                game.graveyardSize(1) shouldBe 4
                game.hasPendingDecision() shouldBe false
            }
        }
    }
}
