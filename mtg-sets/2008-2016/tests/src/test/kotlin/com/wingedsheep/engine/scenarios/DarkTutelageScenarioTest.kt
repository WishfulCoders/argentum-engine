package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Dark Tutelage (M11).
 *
 * "{2}{B} Enchantment — At the beginning of your upkeep, reveal the top card of your library and
 *  put that card into your hand. You lose life equal to its mana value."
 *
 * Dark Confidant's upkeep trigger on an enchantment. Pins the life loss to the *revealed* card's
 * mana value (not a fixed amount), including the zero-mana-value case for a land.
 */
class DarkTutelageScenarioTest : ScenarioTestBase() {

    init {
        context("Dark Tutelage") {

            test("reveals the top card into hand and loses life equal to its mana value") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dark Tutelage")
                    // Library is built top-first: Grizzly Bears (mana value 2) is on top.
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val startLife = game.getLifeTotal(1)
                val startHandSize = game.handSize(1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Grizzly Bears (revealed) should be put into the hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("Hand size should have grown by exactly one") {
                    game.handSize(1) shouldBe startHandSize + 1
                }
                withClue("Life should drop by 2 — Grizzly Bears' mana value") {
                    game.getLifeTotal(1) shouldBe startLife - 2
                }
            }

            test("a revealed land costs 0 life, but the trigger still fires") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dark Tutelage")
                    // Top of library is a land: mana value 0.
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val startLife = game.getLifeTotal(1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("The revealed Mountain should be put into the hand") {
                    game.isInHand(1, "Mountain") shouldBe true
                }
                withClue("A land's mana value is 0, so no life is lost") {
                    game.getLifeTotal(1) shouldBe startLife
                }
            }
        }
    }
}
