package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Thoughtweft Imbuer (ECL) — {3}{W} Creature — Kithkin Advisor, 0/5.
 *
 *   Whenever a creature you control attacks alone, it gets +X/+X until end of turn, where X is
 *   the number of Kithkin you control.
 *
 * "Kithkin you control" counts every Kithkin permanent, not only creatures: Clachan Festival is a
 * Kindred Enchantment — Kithkin. A 17Lands replay (ECL, a lone Feisty Spikeling hitting for 8 with
 * Festival out) showed the engine one short when the count was restricted to creatures.
 */
class ThoughtweftImbuerScenarioTest : ScenarioTestBase() {
    init {
        context("Thoughtweft Imbuer") {
            test("a Kithkin enchantment counts towards X") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thoughtweft Imbuer")
                    .withCardOnBattlefield(1, "Clachan Festival")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Thoughtweft Imbuer" to 2)).error shouldBe null
                game.resolveStack()

                // X = 2 (the Imbuer and the Festival): the 0/5 attacking alone becomes 2/7
                val imbuer = game.findPermanent("Thoughtweft Imbuer").shouldNotBeNull()
                game.state.projectedState.getPower(imbuer) shouldBe 2
            }
        }
    }
}
