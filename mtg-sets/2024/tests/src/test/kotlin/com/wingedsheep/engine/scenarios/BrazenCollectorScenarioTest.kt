package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Brazen Collector (BLB #128) — {1}{R} Creature — Raccoon Rogue, 2/1, first strike.
 *
 *   Whenever this creature attacks, add {R}. Until end of turn, you don't lose this mana as steps
 *   and phases end.
 *
 * Pools empty as each step ends, so the {R} has to be turn-duration mana to reach the second main
 * phase (a 17Lands BLB replay casts Raccoon Rallier with it there), and it is gone the next turn.
 */
class BrazenCollectorScenarioTest : ScenarioTestBase() {
    private fun floating(game: TestGame): Int =
        game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.total ?: 0

    init {
        context("Brazen Collector") {
            test("its attack mana lasts into the second main phase, and not past the turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Brazen Collector")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Brazen Collector" to 2)).error shouldBe null
                game.resolveStack()
                floating(game) shouldBe 1

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                floating(game) shouldBe 1

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                floating(game) shouldBe 0
            }
        }
    }
}
