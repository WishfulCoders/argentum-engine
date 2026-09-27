package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Hatching Plans (GPT #27).
 *
 * "{1}{U} Enchantment
 *  When this enchantment is put into a graveyard from the battlefield, draw three cards."
 *
 * The condition is specifically "from the battlefield" — destroying it there triggers the draw,
 * while it never entering the battlefield at all (e.g. discarded from hand) must not.
 */
class HatchingPlansScenarioTest : ScenarioTestBase() {

    init {
        test("being destroyed on the battlefield draws three cards") {
            val game = scenario()
                .withPlayers("Owner", "Opponent")
                .withCardOnBattlefield(1, "Hatching Plans")
                .withCardInHand(2, "Disenchant")
                .withLandsOnBattlefield(2, "Plains", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)

            val cast = game.castSpell(2, "Disenchant", targetId = game.findPermanent("Hatching Plans"))
            withClue("Casting Disenchant on Hatching Plans should succeed: ${cast.error}") {
                cast.error shouldBe null
            }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Hatching Plans should be destroyed (in its owner's graveyard)") {
                game.isOnBattlefield("Hatching Plans") shouldBe false
                game.isInGraveyard(1, "Hatching Plans") shouldBe true
            }
            withClue("Its controller should have drawn three cards from the trigger") {
                game.handSize(1) shouldBe handBefore + 3
            }
        }

        test("discarding it straight from hand (never having been on the battlefield) does not trigger the draw") {
            val game = scenario()
                .withPlayers("Victim", "Caster")
                .withCardInHand(1, "Hatching Plans")
                .withLandsOnBattlefield(2, "Swamp", 1)
                .withCardInHand(2, "Thoughtseize")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)

            game.castSpellTargetingPlayer(2, "Thoughtseize", targetPlayerNumber = 1)
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            // Thoughtseize's caster (player 2) chooses which nonland card player 1 discards.
            withClue("Thoughtseize should pause for the caster to pick a card from the revealed hand") {
                game.hasPendingDecision() shouldBe true
            }
            val plans = game.findCardsInHand(1, "Hatching Plans").first()
            game.selectCards(listOf(plans))
            game.resolveStack()

            withClue("Hatching Plans should be discarded to the graveyard, having never been on the battlefield") {
                game.isInGraveyard(1, "Hatching Plans") shouldBe true
            }
            withClue("No draw should have happened — the trigger only fires 'from the battlefield'") {
                game.handSize(1) shouldBe handBefore - 1
            }
        }
    }
}
