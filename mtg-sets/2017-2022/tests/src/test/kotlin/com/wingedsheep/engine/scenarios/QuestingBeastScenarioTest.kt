package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Questing Beast (ELD #171).
 *
 * - Can't be blocked by a creature with power 2 or less.
 * - Its combat damage to an opponent is dealt again to target planeswalker *that player* controls
 *   (the damaged opponent — `controlledByTriggeringPlayer()`).
 * - Combat damage from creatures you control ignores prevention, protection's included.
 */
class QuestingBeastScenarioTest : ScenarioTestBase() {

    /** Resolve the stack, answering a target prompt with the opponent's planeswalker. */
    private fun TestGame.resolveTargeting(name: String) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 20) {
            if (state.pendingDecision is ChooseTargetsDecision) selectTargets(listOf(findPermanent(name)!!))
            else if (state.pendingDecision == null) passPriority()
            else error("unexpected decision ${state.pendingDecision}")
        }
    }

    init {
        test("a power-2 creature can't block it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Questing Beast", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Questing Beast" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Questing Beast"))).error shouldNotBe null
        }

        test("combat damage to the opponent is dealt again to a planeswalker that player controls") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Questing Beast")
                .withCardOnBattlefield(2, "Ajani Goldmane")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("haste") { game.declareAttackers(mapOf("Questing Beast" to 2)).error shouldBe null }
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveTargeting("Ajani Goldmane")

            game.getLifeTotal(2) shouldBe 16
            withClue("4 more to the planeswalker: Ajani Goldmane (loyalty 4) dies") {
                game.isOnBattlefield("Ajani Goldmane") shouldBe false
            }
        }

        test("another creature you control's combat damage gets through protection") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Questing Beast")
                .withCardOnBattlefield(1, "Goblin Piker", summoningSickness = false)
                .withCardOnBattlefield(2, "Disciple of Law")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Goblin Piker" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Disciple of Law" to listOf("Goblin Piker"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("protection from red couldn't prevent the Piker's combat damage") {
                game.isOnBattlefield("Disciple of Law") shouldBe false
            }
        }
    }
}
