package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Blade Splicer (NPH #4): its ETB makes a 3/3 Phyrexian Golem artifact token, and Golems you
 * control — only while the Splicer is around — have first strike.
 */
class BladeSplicerScenarioTest : ScenarioTestBase() {

    init {
        context("Blade Splicer") {

            test("enters with a 3/3 Golem token that has first strike; the Splicer itself does not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Blade Splicer")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Blade Splicer").error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                val golems = game.state.getBattlefield().filter { projected.hasSubtype(it, "Golem") }
                golems.size shouldBe 1
                val golem = golems.single()
                projected.getPower(golem) shouldBe 3
                projected.getToughness(golem) shouldBe 3
                projected.hasType(golem, "ARTIFACT") shouldBe true
                projected.hasKeyword(golem, Keyword.FIRST_STRIKE) shouldBe true

                val splicer = game.findPermanent("Blade Splicer")!!
                projected.hasKeyword(splicer, Keyword.FIRST_STRIKE) shouldBe false
            }
        }
    }
}
