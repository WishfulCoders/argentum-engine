package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Reanimate (TMP).
 *
 * Oracle: "Put target creature card from a graveyard onto the battlefield under your control. You
 * lose life equal to that card's mana value."
 *
 * The life loss reads the target's mana value after it has moved to the battlefield; these tests
 * pin that the amount survives the zone change, from either player's graveyard.
 */
class ReanimateScenarioTest : ScenarioTestBase() {

    init {
        context("Reanimate") {
            test("returns a creature from an opponent's graveyard under your control; you lose its mana value") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reanimate")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInGraveyard(2, "Serra Angel")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)
                val opponentLifeBefore = game.getLifeTotal(2)

                val cast = game.castSpellTargetingGraveyardCard(1, "Reanimate", 2, "Serra Angel")
                withClue("Casting Reanimate should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val angel = game.findPermanent("Serra Angel")
                withClue("Serra Angel should be on the battlefield under Player 1's control") {
                    (angel != null) shouldBe true
                    game.state.getEntity(angel!!)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }
                withClue("Player 1 loses 5 life (Serra Angel's mana value); Player 2 is unaffected") {
                    game.getLifeTotal(1) shouldBe lifeBefore - 5
                    game.getLifeTotal(2) shouldBe opponentLifeBefore
                }
            }

            test("returns a creature from your own graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reanimate")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)

                game.castSpellTargetingGraveyardCard(1, "Reanimate", 1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears returns and Player 1 loses 2 life") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.getLifeTotal(1) shouldBe lifeBefore - 2
                }
            }
        }
    }
}
