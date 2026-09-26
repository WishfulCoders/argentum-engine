package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Moonstone Harbinger {2}{B} — Creature — Bat Warrior 1/3
 *   Flying, deathtouch
 *   Whenever you gain or lose life during your turn, Bats you control get +1/+0 and gain
 *   deathtouch until end of turn. This ability triggers only once each turn.
 *
 * "Bats you control" is a bare tribal noun, so the group is every Bat *permanent* you control;
 * the pump lands on the Bat creatures and leaves non-Bats alone.
 */
class MoonstoneHarbingerScenarioTest : ScenarioTestBase() {

    private fun board(activePlayer: Int = 1) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Moonstone Harbinger")
        .withCardOnBattlefield(1, "Mirkwood Bats")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Renewed Faith")
        .withCardInHand(1, "Renewed Faith")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Plains", 6)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(activePlayer)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Moonstone Harbinger") {

            test("gaining life on your turn pumps every Bat you control, not other creatures") {
                val game = board()
                val harbinger = game.findPermanent("Moonstone Harbinger")!!
                val mirkwood = game.findPermanent("Mirkwood Bats")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Renewed Faith").error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("Both Bats get +1/+0") {
                    projected.getPower(harbinger) shouldBe 2
                    projected.getPower(mirkwood) shouldBe 3
                }
                withClue("Mirkwood Bats gains deathtouch") {
                    projected.hasKeyword(mirkwood, Keyword.DEATHTOUCH) shouldBe true
                }
                withClue("Grizzly Bears is not a Bat") {
                    projected.getPower(bears) shouldBe 2
                    projected.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false
                }
            }

            test("triggers only once each turn") {
                val game = board()
                val mirkwood = game.findPermanent("Mirkwood Bats")!!

                game.castSpell(1, "Renewed Faith").error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Renewed Faith").error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(mirkwood) shouldBe 3
            }

            test("losing life on your turn triggers it too") {
                val game = board()
                val mirkwood = game.findPermanent("Mirkwood Bats")!!

                game.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 18
                game.state.projectedState.getPower(mirkwood) shouldBe 3
            }

            test("gaining then losing life in one turn still triggers only once") {
                val game = board()
                val mirkwood = game.findPermanent("Mirkwood Bats")!!

                game.castSpell(1, "Renewed Faith").error shouldBe null
                game.resolveStack()
                game.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
                game.resolveStack()

                withClue("Gain and loss are one ability, so the once-each-turn limit spans both") {
                    game.state.projectedState.getPower(mirkwood) shouldBe 3
                }
            }

            test("gaining life during an opponent's turn does nothing") {
                val game = board(activePlayer = 2)
                val mirkwood = game.findPermanent("Mirkwood Bats")!!

                game.castSpell(1, "Renewed Faith").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 26
                game.state.projectedState.getPower(mirkwood) shouldBe 2
            }
        }
    }
}
