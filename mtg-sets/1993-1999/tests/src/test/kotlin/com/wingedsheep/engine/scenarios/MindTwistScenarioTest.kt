package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mind Twist (LEA #115) — "Target player discards X cards at random."
 */
class MindTwistScenarioTest : ScenarioTestBase() {

    private fun TestGame.castMindTwist(x: Int) {
        val card = findCardsInHand(1, "Mind Twist").single()
        execute(
            CastSpell(player1Id, card, listOf(ChosenTarget.Player(player2Id)), xValue = x)
        ).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay().error shouldBe null
        resolveStack()
    }

    init {
        test("the target player discards exactly X cards") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Mind Twist")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardsInHand(2, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castMindTwist(2)

            withClue("two of four cards discarded, no decision asked") {
                game.hasPendingDecision() shouldBe false
                game.handSize(2) shouldBe 2
                game.graveyardSize(2) shouldBe 2
            }
        }

        test("X larger than the hand discards the whole hand") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Mind Twist")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardsInHand(2, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castMindTwist(5)

            game.handSize(2) shouldBe 0
            game.graveyardSize(2) shouldBe 2
        }
    }
}
