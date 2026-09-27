package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Leonin Relic-Warder (MBS).
 *
 * "{W}{W} Creature — Cat Cleric 2/2
 *  When this creature enters, you may exile target artifact or enchantment.
 *  When this creature leaves the battlefield, return the exiled card to the battlefield under its
 *  owner's control."
 *
 * Pins the two linked triggers (exile on enter, return on leave) and the optional "may" being
 * declinable when a legal target exists.
 */
class LeoninRelicWarderScenarioTest : ScenarioTestBase() {

    init {
        context("Leonin Relic-Warder") {

            test("exiles an artifact on enter and returns it under its owner's control when it dies") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Leonin Relic-Warder")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Voltaic Key")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Leonin Relic-Warder").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack() // resolves the creature; its ETB trigger goes on the stack

                withClue("The ETB trigger should present the optional exile target") {
                    game.hasPendingDecision() shouldBe true
                }
                val key = game.findPermanent("Voltaic Key")!!
                game.selectTargets(listOf(key))
                game.resolveStack()

                withClue("Voltaic Key should be exiled") {
                    game.isOnBattlefield("Voltaic Key") shouldBe false
                    game.isInExile(2, "Voltaic Key") shouldBe true
                }

                // Kill Leonin Relic-Warder (2 toughness) with Lightning Bolt (3 damage).
                val relicWarder = game.findPermanent("Leonin Relic-Warder")!!
                game.castSpell(1, "Lightning Bolt", targetId = relicWarder).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack() // resolves the bolt; Relic-Warder dies; LTB trigger goes on stack
                game.resolveStack() // resolves the LTB return trigger

                withClue("Voltaic Key should return to the battlefield under its owner's (player 2's) control") {
                    game.isOnBattlefield("Voltaic Key") shouldBe true
                    game.isInExile(2, "Voltaic Key") shouldBe false
                }
            }

            test("the exile may be declined even though a legal target exists") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Leonin Relic-Warder")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Voltaic Key")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Leonin Relic-Warder").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.skipTargets()
                game.resolveStack()

                withClue("Declining the may-exile leaves the artifact on the battlefield") {
                    game.isOnBattlefield("Voltaic Key") shouldBe true
                    game.isInExile(2, "Voltaic Key") shouldBe false
                }
            }
        }
    }
}
