package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Show and Tell (USG #96) — each player may put an artifact, creature, enchantment, or land card
 * from their hand onto the battlefield. The active player chooses first, then each other player;
 * the chosen cards enter together, each under its owner's control.
 */
class ShowAndTellScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withCardInHand(1, "Show and Tell")
            .withLandsOnBattlefield(1, "Island", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("both players put a card in, each under its owner's control, and nothing enters before both choose") {
            val game = base()
                .withCardInHand(1, "Hill Giant")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(2, "Grizzly Bears")
                .build()

            game.castSpell(1, "Show and Tell").error shouldBe null
            game.resolveStack()

            withClue("the active player is asked first") {
                game.getPendingDecision()?.playerId shouldBe game.player1Id
            }
            game.selectCards(game.findCardsInHand(1, "Hill Giant")).error shouldBe null

            withClue("the opponent chooses next, and the giant has not entered yet") {
                game.getPendingDecision()?.playerId shouldBe game.player2Id
                game.isOnBattlefield("Hill Giant") shouldBe false
            }
            game.selectCards(game.findCardsInHand(2, "Grizzly Bears")).error shouldBe null

            val giant = game.findPermanent("Hill Giant")
            val bears = game.findPermanent("Grizzly Bears")
            giant shouldNotBe null
            bears shouldNotBe null
            game.state.getEntity(giant!!)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            game.state.getEntity(bears!!)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
            withClue("an instant is not a legal choice and stays in hand") {
                game.isInHand(1, "Lightning Bolt") shouldBe true
            }
        }

        test("either player may decline") {
            val game = base()
                .withCardInHand(1, "Hill Giant")
                .withCardInHand(2, "Grizzly Bears")
                .build()

            game.castSpell(1, "Show and Tell").error shouldBe null
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.selectCards(game.findCardsInHand(2, "Grizzly Bears")).error shouldBe null

            game.isInHand(1, "Hill Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Show and Tell") shouldBe true
        }
    }
}
