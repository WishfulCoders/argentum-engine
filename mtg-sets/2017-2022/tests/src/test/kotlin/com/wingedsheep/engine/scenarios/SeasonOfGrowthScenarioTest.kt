package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Season of Growth (M20 #191) — {1}{G} Enchantment.
 *
 * "Whenever a creature you control enters, scry 1.
 *  Whenever you cast a spell that targets a creature you control, draw a card."
 *
 * Two independent triggers. Covers the creature-enters half firing scry 1 (draining both scry
 * prompts, keeping order), and the cast-trigger half: it draws when the targeted creature is yours,
 * and doesn't when the same spell targets an opponent's creature instead.
 */
class SeasonOfGrowthScenarioTest : ScenarioTestBase() {

    // Drains scry's decision(s) without changing library order — same pattern used elsewhere for
    // Scry (SelectCardsDecision for the bottom pick, then a ReorderLibraryDecision for the rest).
    private fun ScenarioTestBase.TestGame.drainScryDecisions() {
        var iterations = 0
        while (hasPendingDecision() && iterations++ < 4) {
            when (val decision = getPendingDecision()) {
                is SelectCardsDecision -> skipSelection()
                is ReorderLibraryDecision -> keepLibraryOrder()
                else -> return
            }
        }
    }

    init {
        context("Season of Growth") {

            test("a creature you control entering triggers scry 1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Season of Growth")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val librarySizeBefore = game.librarySize(1)

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.drainScryDecisions()
                game.resolveStack()

                withClue("scry 1 looked at a card but nothing was drawn or lost") {
                    game.librarySize(1) shouldBe librarySizeBefore
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("casting a spell that targets a creature you control draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Season of Growth")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                game.castSpell(1, "Giant Growth", bears).error shouldBe null
                game.resolveStack()

                withClue("net hand change: -1 (Giant Growth leaves) +1 (draw) = 0") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("casting a spell that targets an opponent's creature draws nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Season of Growth")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                game.castSpell(1, "Giant Growth", bears).error shouldBe null
                game.resolveStack()

                withClue("targeting an opponent's creature does not satisfy 'a creature you control'") {
                    game.handSize(1) shouldBe handBefore - 1
                }
            }
        }
    }
}
