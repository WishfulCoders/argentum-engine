package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Parallel Lives (ISD #199).
 *
 * "{3}{G} Enchantment
 *  If an effect would create one or more tokens under your control, it creates twice that many of
 *  those tokens instead."
 *
 * Midnight Haunting ("Create two 1/1 white Spirit creature tokens with flying") is the token
 * source. Per the 2023-09-01 ruling, two Parallel Lives compound (2x becomes 4x, not 3x), which
 * the second test pins down.
 */
class ParallelLivesScenarioTest : ScenarioTestBase() {

    init {
        test("doubles the number of tokens created") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Parallel Lives")
                .withCardInHand(1, "Midnight Haunting")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val before = game.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD)).toSet()

            val cast = game.castSpell(1, "Midnight Haunting")
            withClue("Casting Midnight Haunting should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val tokens = game.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD)).filter { it !in before }
            withClue("Midnight Haunting normally makes two tokens; Parallel Lives should double that to four") {
                tokens.size shouldBe 4
            }
            withClue("Every doubled token should still be a 1/1 Spirit") {
                tokens.forEach { id ->
                    game.state.projectedState.getPower(id) shouldBe 1
                    game.state.projectedState.getToughness(id) shouldBe 1
                }
            }
        }

        test("two Parallel Lives compound to quadruple, not triple") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Parallel Lives")
                .withCardOnBattlefield(1, "Parallel Lives")
                .withCardInHand(1, "Midnight Haunting")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val before = game.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD)).toSet()

            game.castSpell(1, "Midnight Haunting")
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val tokens = game.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD)).filter { it !in before }
            withClue("Two Parallel Lives replacement effects stack multiplicatively: 2 -> 4 -> 8") {
                tokens.size shouldBe 8
            }
        }

        test("a token created under an opponent's control is not doubled") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Parallel Lives")
                .withCardInHand(2, "Midnight Haunting")
                .withLandsOnBattlefield(2, "Plains", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val before = game.state.getZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD)).toSet()

            game.castSpell(2, "Midnight Haunting")
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val tokens = game.state.getZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD)).filter { it !in before }
            withClue("Parallel Lives only doubles tokens created under its controller's control") {
                tokens.size shouldBe 2
            }
        }
    }
}
