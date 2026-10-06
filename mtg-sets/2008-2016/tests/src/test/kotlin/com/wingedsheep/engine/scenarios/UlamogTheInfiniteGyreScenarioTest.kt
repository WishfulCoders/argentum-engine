package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ulamog, the Infinite Gyre (ROE #12) — {11} Legendary Creature — Eldrazi 10/10
 *
 *   When you cast this spell, destroy target permanent.
 *   Indestructible
 *   Annihilator 4
 *   When Ulamog is put into a graveyard from anywhere, its owner shuffles their graveyard into
 *   their library.
 *
 * The cast trigger resolves before Ulamog does; the graveyard shuffle fires from any zone, here
 * from the library (milled), and takes the whole graveyard with it — Ulamog included.
 */
class UlamogTheInfiniteGyreScenarioTest : ScenarioTestBase() {
    init {
        test("the cast trigger destroys the target permanent before Ulamog resolves") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Ulamog, the Infinite Gyre")
                .withLandsOnBattlefield(1, "Island", 11)
                .withCardOnBattlefield(2, "Juggernaut")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Ulamog, the Infinite Gyre").error shouldBe null
            if (game.hasPendingDecision()) {
                game.selectTargets(listOf(game.findPermanent("Juggernaut")!!)).error shouldBe null
            }
            withClue("the trigger sits above the spell") {
                game.state.stack.size shouldBe 2
            }
            game.passPriority()
            game.passPriority()
            withClue("Juggernaut is destroyed while Ulamog is still on the stack") {
                game.isInGraveyard(2, "Juggernaut") shouldBe true
                game.isOnBattlefield("Ulamog, the Infinite Gyre") shouldBe false
            }
            game.resolveStack()
            game.isOnBattlefield("Ulamog, the Infinite Gyre") shouldBe true
        }

        test("milled from the library, Ulamog shuffles its owner's whole graveyard into the library") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Mental Note")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(1, "Ulamog, the Infinite Gyre")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Mental Note").error shouldBe null
            game.resolveStack()

            withClue("Mental Note milled Ulamog and Hill Giant and drew Grizzly Bears") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
            withClue("the trigger shuffled the graveyard — Ulamog, the Giant, the Bolt and Mental Note — away") {
                game.graveyardSize(1) shouldBe 0
                game.librarySize(1) shouldBe 4
                game.findCardsInLibrary(1, "Ulamog, the Infinite Gyre").size shouldBe 1
            }
        }
    }
}
