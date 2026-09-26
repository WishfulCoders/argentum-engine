package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Reclusive Wight (USG #153) — {3}{B} Creature — Zombie Minion, 4/4.
 *
 *   At the beginning of your upkeep, if you control another nonland permanent, sacrifice this
 *   creature.
 *
 * Lands never count, and neither does the Wight itself.
 */
class ReclusiveWightScenarioTest : ScenarioTestBase() {
    init {
        context("Reclusive Wight") {
            test("with only lands beside it, it stays") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Reclusive Wight")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.findPermanents("Reclusive Wight").size shouldBe 1
            }

            test("another nonland permanent makes its controller sacrifice it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Reclusive Wight")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.findPermanents("Reclusive Wight").size shouldBe 0
            }
        }
    }
}
