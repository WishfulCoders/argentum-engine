package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Archon of Cruelty (MH2 #75) — "Whenever this creature enters or attacks, target opponent sacrifices
 * a creature or planeswalker of their choice, discards a card, and loses 3 life. You draw a card and
 * gain 3 life."
 */
class ArchonOfCrueltyScenarioTest : ScenarioTestBase() {

    private fun TestGame.drain() {
        var guard = 0
        while ((state.stack.isNotEmpty() || getPendingDecision() != null) && guard++ < 30) {
            when (val d = getPendingDecision()) {
                null -> resolveStack()
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay().error shouldBe null
                is ChooseTargetsDecision -> selectTargets(listOf(player2Id)).error shouldBe null
                is SelectCardsDecision -> selectCards(d.options.take(d.minSelections)).error shouldBe null
                else -> error("unexpected decision ${d::class.simpleName}")
            }
        }
    }

    init {
        test("entering: the opponent sacrifices, discards and loses 3; you draw and gain 3") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Archon of Cruelty")
                .withLandsOnBattlefield(1, "Swamp", 8)
                .withCardInLibrary(1, "Swamp")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Archon of Cruelty").error shouldBe null
            game.drain()

            withClue("Archon resolved") { game.isOnBattlefield("Archon of Cruelty") shouldBe true }
            withClue("opponent sacrificed their creature") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
            withClue("opponent discarded") {
                game.handSize(2) shouldBe 0
                game.isInGraveyard(2, "Forest") shouldBe true
            }
            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 23
            withClue("you drew a card") { game.handSize(1) shouldBe 1 }
        }

        test("attacking triggers it again") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Archon of Cruelty")
                .withCardInLibrary(1, "Swamp")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Archon of Cruelty" to 2)).error shouldBe null
            game.drain()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.handSize(2) shouldBe 0
            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 23
        }
    }
}
