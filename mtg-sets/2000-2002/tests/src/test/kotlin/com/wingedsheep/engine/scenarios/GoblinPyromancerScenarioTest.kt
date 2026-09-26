package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Goblin Pyromancer {3}{R} — Creature — Goblin Wizard 2/2
 *   When Goblin Pyromancer enters the battlefield, Goblin creatures get +3/+0 until end of turn.
 *   At the beginning of the end step, destroy all Goblins.
 *
 * The two sentences name different groups: "Goblin creatures" is adjectival (creatures only),
 * while the bare noun "Goblins" is every Goblin *permanent* — a Kindred Enchantment — Goblin
 * is destroyed too.
 */
class GoblinPyromancerScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin Pyromancer end-step trigger") {

            test("destroys every Goblin permanent, including a noncreature Goblin") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin Pyromancer")
                    .withCardOnBattlefield(2, "Boggart Mischief")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("The Goblin creature is destroyed") {
                    game.isInGraveyard(1, "Goblin Pyromancer") shouldBe true
                }
                withClue("The Kindred Enchantment — Goblin is destroyed") {
                    game.isInGraveyard(2, "Boggart Mischief") shouldBe true
                }
                withClue("A non-Goblin survives") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
