package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Journey to Nowhere (ZEN).
 *
 * "{1}{W} Enchantment
 *  When this enchantment enters, exile target creature.
 *  When this enchantment leaves the battlefield, return the exiled card to the battlefield under
 *  its owner's control."
 *
 * Oblivion Ring's two separate triggers: exile on enter, return on leave. Pins the round trip and,
 * per the 2009-10-01 ruling, that destroying Journey to Nowhere *before* its enters trigger
 * resolves exiles the creature for good (the leaves trigger finds nothing to return).
 */
class JourneyToNowhereScenarioTest : ScenarioTestBase() {

    init {
        test("exiles the targeted creature and returns it when Journey to Nowhere leaves") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Journey to Nowhere")
                .withCardInHand(1, "Disenchant")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Journey to Nowhere").error shouldBe null
            if (game.hasPendingDecision() && game.getPendingDecision() !is ChooseTargetsDecision) {
                game.submitManaSourcesAutoPay()
            }
            game.resolveStack()
            withClue("the enters trigger asks for its target") {
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears should be exiled") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }

            val journey = game.findPermanent("Journey to Nowhere")!!
            game.castSpell(1, "Disenchant", targetId = journey).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Grizzly Bears returns to the battlefield under its owner's control") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe false
            }
        }

        test("exiles the creature forever if Journey to Nowhere leaves before its enters trigger resolves") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Journey to Nowhere")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(2, "Disenchant")
                .withLandsOnBattlefield(2, "Plains", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Journey to Nowhere").error shouldBe null
            if (game.hasPendingDecision() && game.getPendingDecision() !is ChooseTargetsDecision) {
                game.submitManaSourcesAutoPay()
            }
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null

            // The enters trigger is on the stack. The caster passes; the victim destroys the Journey
            // in response, so its leaves trigger resolves first and finds nothing to return.
            game.passPriority().error shouldBe null
            val journey = game.findPermanent("Journey to Nowhere")!!
            game.castSpell(2, "Disenchant", targetId = journey).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Journey to Nowhere was destroyed") {
                game.isOnBattlefield("Journey to Nowhere") shouldBe false
            }
            withClue("the enters trigger still exiled Grizzly Bears, and nothing brings it back") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }
        }
    }
}
