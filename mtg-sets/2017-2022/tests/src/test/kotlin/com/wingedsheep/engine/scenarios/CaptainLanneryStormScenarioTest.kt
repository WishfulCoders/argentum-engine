package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Captain Lannery Storm (XLN #136).
 *
 * "{2}{R} Legendary Creature — Human Pirate, 2/2, Haste. Whenever Captain Lannery Storm attacks,
 * create a Treasure token. Whenever you sacrifice a Treasure, Captain Lannery Storm gets +1/+0
 * until end of turn."
 *
 * Exercises both triggers separately: attacking creates exactly one Treasure, and sacrificing a
 * Treasure (via its own mana ability, not just to cast a spell) pumps the Captain's power.
 */
class CaptainLanneryStormScenarioTest : ScenarioTestBase() {

    init {
        context("Captain Lannery Storm") {

            test("attacking creates a Treasure token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Captain Lannery Storm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("no Treasure before attacking") {
                    game.findPermanents("Treasure").size shouldBe 0
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Captain Lannery Storm" to 2)).error shouldBe null
                game.resolveStack()

                withClue("attacking creates exactly one Treasure") {
                    game.findPermanents("Treasure").size shouldBe 1
                }
            }

            test("sacrificing a Treasure gives the Captain +1/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Captain Lannery Storm")
                    .withCardOnBattlefield(1, "Treasure", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val captain = game.findPermanent("Captain Lannery Storm")!!
                val treasure = game.findPermanent("Treasure")!!

                withClue("before sacrifice, the Captain is a plain 2/2") {
                    game.state.projectedState.getPower(captain) shouldBe 2
                }

                val activation = game.getLegalActions(1).firstOrNull { info ->
                    (info.action as? ActivateAbility)?.sourceId == treasure
                }?.action ?: error("expected Treasure's mana ability to be a legal action")

                game.execute(activation).error shouldBe null
                if (game.getPendingDecision() is ChooseColorDecision) {
                    val decision = game.getPendingDecision() as ChooseColorDecision
                    game.submitDecision(ColorChosenResponse(decision.id, Color.RED))
                }
                game.resolveStack()

                withClue("sacrificing the Treasure (for its own mana ability) triggers the +1/+0 pump") {
                    game.findPermanent("Treasure") shouldBe null
                    game.state.projectedState.getPower(captain) shouldBe 3
                }
            }
        }
    }
}
