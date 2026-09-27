package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deadly Dispute (AFR #94) — {1}{B} Instant.
 *
 * "As an additional cost to cast this spell, sacrifice an artifact or creature.
 *  Draw two cards and create a Treasure token."
 *
 * Pins the additional-cost gate (either an artifact or a creature satisfies it) and the payoff:
 * two cards drawn plus a Treasure token created under the caster's control, on top of the
 * sacrificed permanent actually leaving for the graveyard.
 */
class DeadlyDisputeScenarioTest : ScenarioTestBase() {

    init {
        context("Deadly Dispute") {

            test("sacrificing a creature draws two cards and creates a Treasure") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Deadly Dispute")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.castSpellWithAdditionalSacrifice(1, "Deadly Dispute", "Grizzly Bears")
                    .error shouldBe null
                game.resolveStack()

                withClue("the sacrificed creature is in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("net hand change: -1 (Dispute leaves) +2 (draw) = +1") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                withClue("a Treasure token is created under the caster's control") {
                    game.findAllPermanents("Treasure").size shouldBe 1
                }
            }

            test("sacrificing an artifact also satisfies the cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Deadly Dispute")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Prophetic Prism")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.castSpellWithAdditionalSacrifice(1, "Deadly Dispute", "Prophetic Prism")
                    .error shouldBe null
                game.resolveStack()

                withClue("the sacrificed artifact is in the graveyard") {
                    game.isInGraveyard(1, "Prophetic Prism") shouldBe true
                }
                withClue("draws two, creates a Treasure, same as sacrificing a creature") {
                    game.handSize(1) shouldBe handBefore + 1
                    game.findAllPermanents("Treasure").size shouldBe 1
                }
            }
        }
    }
}
