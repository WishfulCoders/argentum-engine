package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Cut Down (DMU #89).
 *
 * {B} Instant
 * "Destroy target creature with total power and toughness 5 or less."
 *
 * Covers the legal target (2/2 → 4), the illegal one (3/3 → 6), and that the filter reads
 * projected stats: a 2/2 pumped to 5/5 is no longer a legal target.
 */
class CutDownScenarioTest : ScenarioTestBase() {

    init {
        context("Cut Down") {

            test("destroys a creature with total power and toughness 5 or less") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Cut Down")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Cut Down", bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("can't target a creature with total power and toughness greater than 5") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Cut Down")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Cut Down", giant).error shouldNotBe null
                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("reads projected stats — a pumped 2/2 is out of range") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Cut Down")
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Giant Growth", bears).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Cut Down", bears).error shouldNotBe null
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
