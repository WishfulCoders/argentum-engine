package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Heartless Pillage (XLN #109).
 *
 * "{2}{B} Sorcery — Target opponent discards two cards. Raid — If you attacked this turn, create a
 * Treasure token."
 *
 * The raid Treasure is unconditional on the discard's outcome (2020-08-07 ruling: you still get it
 * even if the opponent discards fewer than two cards), so the tests separate "attacked → Treasure"
 * from "discards exactly two, or as many as available."
 */
class HeartlessPillageScenarioTest : ScenarioTestBase() {

    init {
        context("Heartless Pillage") {

            test("raid: attacking this turn creates a Treasure, and the opponent discards two cards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Heartless Pillage")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Plains")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                val handSizeBefore = game.handSize(2)
                val cast = game.castSpellTargetingPlayer(1, "Heartless Pillage", 2)
                withClue("Casting Heartless Pillage should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                // With three cards in hand the opponent chooses which two to discard.
                (game.getPendingDecision() as? com.wingedsheep.engine.core.SelectCardsDecision)?.let { choice ->
                    game.selectCards(choice.options.take(2))
                    game.resolveStack()
                }

                withClue("the opponent discards two cards") {
                    game.handSize(2) shouldBe handSizeBefore - 2
                }
                withClue("having attacked this turn creates exactly one Treasure") {
                    game.findPermanents("Treasure").size shouldBe 1
                }
            }

            test("without attacking this turn, no Treasure is created") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Heartless Pillage")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellTargetingPlayer(1, "Heartless Pillage", 2)
                withClue("Casting Heartless Pillage should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the opponent still discards their (fewer than two) remaining cards") {
                    game.handSize(2) shouldBe 0
                }
                withClue("no attack this turn — the raid condition fails, so no Treasure") {
                    game.findPermanents("Treasure").size shouldBe 0
                }
            }
        }
    }
}
