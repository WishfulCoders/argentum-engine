package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class BurningOfXinyeScenarioTest : ScenarioTestBase() {
    init {
        context("Burning of Xinye") {
            test("each player destroys four of their own lands, then every creature takes 4") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Burning of Xinye")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(2, "Plains", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Burning of Xinye", 2).error shouldBe null
                game.resolveStack()

                var guard = 0
                val askedPlayers = mutableListOf<com.wingedsheep.sdk.model.EntityId>()
                while (game.hasPendingDecision() && guard++ < 6) {
                    val d = game.getPendingDecision() as SelectCardsDecision
                    askedPlayers += d.playerId
                    withClue("exactly four lands are chosen") { d.minSelections shouldBe 4 }
                    game.selectCards(d.options.take(4))
                }
                withClue("both players chose, controller first") {
                    askedPlayers shouldBe listOf(game.player1Id, game.player2Id)
                }
                game.findAllPermanents("Mountain").size + game.findAllPermanents("Forest").size shouldBe 3
                game.findAllPermanents("Plains").size shouldBe 1
                withClue("all creatures took 4") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("a player with fewer than four lands destroys them all") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Burning of Xinye")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Burning of Xinye", 2).error shouldBe null
                game.resolveStack()
                var guard = 0
                while (game.hasPendingDecision() && guard++ < 6) {
                    val d = game.getPendingDecision() as SelectCardsDecision
                    game.selectCards(d.options.take(4))
                }
                game.findAllPermanents("Plains").size shouldBe 0
                game.findAllPermanents("Mountain").size shouldBe 2
            }
        }
    }
}
