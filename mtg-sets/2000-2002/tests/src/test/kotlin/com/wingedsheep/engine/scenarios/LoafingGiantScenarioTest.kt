package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Loafing Giant (INV #153) — "Whenever this creature attacks or blocks, mill a card. If a land card
 * was milled this way, prevent all combat damage this creature would deal this turn."
 *
 * The prevention names *this creature* only. It used to be spelled as a source-group shield over
 * `GroupFilter.source()`, whose `Self` scope the engine never read — so a milled land silenced the
 * combat damage of every permanent on the battlefield. A second attacker pins the fix.
 */
class LoafingGiantScenarioTest : ScenarioTestBase() {

    private fun attackAfterMilling(topCard: String): Pair<TestGame, Int> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Loafing Giant", summoningSickness = false) // 4/6
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
            .withCardInLibrary(1, topCard)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val before = game.getLifeTotal(2)
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Loafing Giant" to 2, "Grizzly Bears" to 2)).error shouldBe null
        game.resolveStack()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers()
        game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        return game to before
    }

    init {
        context("Loafing Giant") {
            test("milling a land prevents only the Giant's combat damage") {
                val (game, before) = attackAfterMilling("Forest")

                withClue("the land was milled") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                }
                withClue("the Giant's 4 is prevented; the Bears' 2 still connects") {
                    game.getLifeTotal(2) shouldBe before - 2
                }
            }

            test("milling a nonland leaves the Giant's damage alone") {
                val (game, before) = attackAfterMilling("Grizzly Bears")

                withClue("both attackers connect: 4 + 2") {
                    game.getLifeTotal(2) shouldBe before - 6
                }
            }
        }
    }
}
