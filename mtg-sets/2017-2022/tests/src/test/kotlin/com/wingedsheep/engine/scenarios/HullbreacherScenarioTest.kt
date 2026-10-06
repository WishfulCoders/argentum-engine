package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hullbreacher (CMR #74). The draw-step exemption itself is pinned by the engine's
 * `DrawReplacementDrawStepExemptionTest`; this covers the card: flash, the Treasure going to
 * Hullbreacher's controller, and the Narset interaction (CR 614.17c — a forbidden draw can't be
 * replaced, while a replaced draw doesn't use up the opponent's one allowed draw).
 */
class HullbreacherScenarioTest : ScenarioTestBase() {

    init {
        fun treasures(game: TestGame, playerNumber: Int): Int {
            val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
            return game.state.getBattlefield(playerId).count {
                game.state.getEntity(it)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Treasure"
            }
        }

        test("an opponent's draws outside their draw step become Treasures for Hullbreacher's controller") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hullbreacher")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 4)
                .apply { repeat(4) { withCardInLibrary(2, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()

            game.handSize(2) shouldBe 0
            game.librarySize(2) shouldBe 4
            treasures(game, 1) shouldBe 2
            treasures(game, 2) shouldBe 0
        }

        test("flash: cast on the opponent's turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hullbreacher")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Hullbreacher").error shouldBe null
            game.resolveStack()
            (game.findPermanent("Hullbreacher") != null) shouldBe true
        }

        test("with Narset: a replaced draw doesn't use up the allowance, a forbidden one makes no Treasure") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hullbreacher")
                .withCardOnBattlefield(1, "Narset, Parter of Veils")
                .withCardsInHand(1, "Inspiration", 2)
                .withLandsOnBattlefield(1, "Island", 8)
                .apply { repeat(4) { withCardInLibrary(2, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()
            withClue("both draws were replaced, so neither counted against Narset's one draw") {
                treasures(game, 1) shouldBe 2
                game.handSize(2) shouldBe 0
            }
        }

        test("with Narset: once the opponent has drawn, further draws are forbidden and can't be replaced") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hullbreacher")
                .withCardOnBattlefield(1, "Narset, Parter of Veils")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardsDrawnThisTurn(2, 1)
                .apply { repeat(4) { withCardInLibrary(2, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()
            treasures(game, 1) shouldBe 0
            game.handSize(2) shouldBe 0
        }
    }
}
