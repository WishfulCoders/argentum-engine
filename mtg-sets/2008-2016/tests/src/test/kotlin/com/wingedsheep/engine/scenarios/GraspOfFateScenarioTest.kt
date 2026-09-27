package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Grasp of Fate (C15 #3).
 *
 * "{1}{W}{W} Enchantment
 *  When this enchantment enters, for each opponent, exile up to one target nonland permanent that
 *  player controls until this enchantment leaves the battlefield. (Those permanents return under
 *  their owners' control.)"
 *
 * In a two-player game "for each opponent" is a single optional target on the one opponent. Pins
 * the exile-until-leaves duration (the permanent returns the instant Grasp of Fate itself leaves)
 * and the "up to one" wording (declining the target exiles nothing).
 */
class GraspOfFateScenarioTest : ScenarioTestBase() {

    init {
        test("exiles the chosen opponent permanent and returns it when Grasp of Fate leaves") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Grasp of Fate")
                .withCardInHand(1, "Disenchant")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            val cast = game.castSpell(1, "Grasp of Fate")
            withClue("Casting Grasp of Fate should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("The ETB trigger should pause for the opponent-permanent target") {
                game.hasPendingDecision() shouldBe true
            }
            game.selectTargets(listOf(bears))
            game.resolveStack()

            withClue("Grizzly Bears should be exiled") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }

            // Destroy Grasp of Fate — the exiled permanent should come back immediately.
            val grasp = game.findPermanent("Grasp of Fate")!!
            game.castSpell(1, "Disenchant", targetId = grasp)
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Grasp of Fate should be destroyed") {
                game.isOnBattlefield("Grasp of Fate") shouldBe false
            }
            withClue("Grizzly Bears should return to the battlefield under its owner's control") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe false
            }
        }

        test("declining the optional target exiles nothing") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Grasp of Fate")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Grasp of Fate")
            withClue("Casting Grasp of Fate should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("The ETB trigger should still pause, offering an optional target") {
                game.hasPendingDecision() shouldBe true
            }
            game.skipTargets()
            game.resolveStack()

            withClue("Nothing should have been exiled") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe false
            }
        }
    }
}
