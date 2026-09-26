package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Wax-Wane Witness {3}{W} — Creature — Bat Cleric 2/4
 *   Flying, vigilance
 *   Whenever you gain or lose life during your turn, this creature gets +1/+0 until end of turn.
 *
 * One ability over both directions, with no per-turn limit: every gain and every loss on your
 * turn triggers it.
 */
class WaxWaneWitnessScenarioTest : ScenarioTestBase() {

    private fun board(activePlayer: Int = 1) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Wax-Wane Witness")
        .withCardInHand(1, "Renewed Faith")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(activePlayer)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Wax-Wane Witness") {

            test("gaining and then losing life on your turn triggers once for each") {
                val game = board()
                val witness = game.findPermanent("Wax-Wane Witness")!!

                game.castSpell(1, "Renewed Faith").error shouldBe null
                game.resolveStack()
                game.state.projectedState.getPower(witness) shouldBe 3

                game.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getPower(witness) shouldBe 4
            }

            test("gaining life during an opponent's turn does nothing") {
                val game = board(activePlayer = 2)
                val witness = game.findPermanent("Wax-Wane Witness")!!

                game.castSpell(1, "Renewed Faith").error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(witness) shouldBe 2
            }
        }
    }
}
