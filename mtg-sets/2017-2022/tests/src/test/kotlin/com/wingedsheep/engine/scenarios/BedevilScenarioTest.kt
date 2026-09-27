package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bedevil (RNA #157).
 *
 * "{B}{B}{R} Instant — Destroy target artifact, creature, or planeswalker."
 *
 * The three-way target filter (Artifact or Creature or Planeswalker) is a single
 * [com.wingedsheep.sdk.scripting.GameObjectFilter] union; these tests exercise the creature and
 * artifact legs of that union (destroying Grizzly Bears and Sol Ring respectively). The
 * planeswalker leg shares the same `Effects.Destroy` resolution path and isn't separately covered.
 */
class BedevilScenarioTest : ScenarioTestBase() {

    init {
        context("Bedevil") {

            test("destroys a targeted creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bedevil")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpell(1, "Bedevil", targetId = bears)
                withClue("Casting Bedevil at Grizzly Bears should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Grizzly Bears is destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("destroys a targeted artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bedevil")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val solRing = game.findPermanent("Sol Ring")!!

                val cast = game.castSpell(1, "Bedevil", targetId = solRing)
                withClue("Casting Bedevil at Sol Ring should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Sol Ring is destroyed") {
                    game.isOnBattlefield("Sol Ring") shouldBe false
                    game.isInGraveyard(2, "Sol Ring") shouldBe true
                }
            }
        }
    }
}
