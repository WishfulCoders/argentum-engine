package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Light Up the Stage (RNA #107).
 *
 * "{2}{R} Sorcery — Spectacle {R} (You may cast this spell for its spectacle cost rather than its
 * mana cost if an opponent lost life this turn.) Exile the top two cards of your library. Until
 * the end of your next turn, you may play those cards."
 *
 * Pins: the spectacle alternative cost is only payable once an opponent has actually lost life
 * this turn (here, from a resolved Lightning Bolt), and is otherwise unavailable; and the
 * unconditional part of the effect (exiling the top two library cards) fires regardless of which
 * cost was paid.
 */
class LightUpTheStageScenarioTest : ScenarioTestBase() {

    init {
        context("Light Up the Stage") {

            test("spectacle cost is payable once an opponent lost life this turn, and exiles the top two cards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Light Up the Stage")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Make an opponent lose life this turn.
                val bolt = game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                withClue("Lightning Bolt should resolve: ${bolt.error}") { bolt.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                withClue("setup: player 2 lost life this turn") {
                    game.getLifeTotal(2) shouldBe 17
                }

                val librarySizeBefore = game.librarySize(1)

                val cast = game.castSpellWithAlternativeCost(1, "Light Up the Stage")
                withClue("Spectacle cost {R} should be payable: ${cast.error}") {
                    cast.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the top two library cards are exiled") {
                    game.librarySize(1) shouldBe librarySizeBefore - 2
                    game.state.getExile(game.player1Id).size shouldBe 2
                }
            }

            test("without an opponent having lost life this turn, the spectacle cost is unavailable") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Light Up the Stage")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellWithAlternativeCost(1, "Light Up the Stage")
                withClue("no opponent has lost life this turn, so spectacle cannot be paid") {
                    (cast.error != null) shouldBe true
                }
                withClue("the card is still in hand — the illegal cast was rejected") {
                    game.isInHand(1, "Light Up the Stage") shouldBe true
                }
            }
        }
    }
}
