package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Stab Wound (RTR).
 *
 * "{2}{B} Enchantment — Aura. Enchant creature. Enchanted creature gets -2/-2.
 *  At the beginning of the upkeep of enchanted creature's controller, that player loses 2 life."
 *
 * The upkeep trigger is bound to the enchanted creature (not to Stab Wound's controller), so it
 * fires on the *enchanted creature's controller's* upkeep — this is the case worth pinning when the
 * Aura's controller and the enchanted creature's controller differ, as when it's cast on an
 * opponent's creature.
 */
class StabWoundScenarioTest : ScenarioTestBase() {

    init {
        context("Stab Wound") {

            test("the enchanted creature gets -2/-2") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Stab Wound", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Grizzly Bears is printed 2/2; Stab Wound's -2/-2 should reduce it to 0/0") {
                    projected.getPower(bears) shouldBe 0
                    projected.getToughness(bears) shouldBe 0
                }
            }

            test("at the beginning of the enchanted creature controller's upkeep, that player loses 2 life") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    // Give the enchanted creature enough toughness to survive -2/-2.
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Stab Wound", "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val casterStartLife = game.getLifeTotal(1)
                val victimStartLife = game.getLifeTotal(2)

                // The scenario is built at player 1's precombat main, i.e. after player 1's own
                // upkeep already passed for turn 1 — so the next upkeep this advances to is
                // player 2's, which is exactly the *enchanted creature's controller's* upkeep.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("sanity: this should be player 2's upkeep") {
                    game.state.activePlayerId shouldBe game.player2Id
                }
                game.resolveStack()

                withClue("Player 2 (the enchanted creature's controller) should lose 2 life") {
                    game.getLifeTotal(2) shouldBe victimStartLife - 2
                }
                withClue("Player 1 (Stab Wound's controller) is unaffected") {
                    game.getLifeTotal(1) shouldBe casterStartLife
                }
            }
        }
    }
}
