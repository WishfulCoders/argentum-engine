package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bitterblossom (Morningtide).
 *
 * "{1}{B} Kindred Enchantment — Faerie
 *  At the beginning of your upkeep, you lose 1 life and create a 1/1 black Faerie Rogue creature
 *  token with flying."
 *
 * Per the 2008-04-01 ruling the life loss isn't a payment: pins that the token is created even at
 * 1 life remaining (the trigger doesn't get skipped or fizzle for lack of life to "pay").
 */
class BitterblossomScenarioTest : ScenarioTestBase() {

    init {
        context("Bitterblossom") {

            test("at upkeep, the controller loses 1 life and gets a 1/1 flying Faerie Rogue token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bitterblossom")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val startLife = game.getLifeTotal(1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("The controller should lose exactly 1 life") {
                    game.getLifeTotal(1) shouldBe startLife - 1
                }
                withClue("A Faerie Rogue token should be on the battlefield") {
                    game.isOnBattlefield("Faerie Rogue Token") shouldBe true
                }
            }

            test("at 1 life the loss is still taken and the token still made") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bitterblossom")
                    .withLifeTotal(1, 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("The life loss is mandatory and not a payment — it applies even at 1 life") {
                    game.getLifeTotal(1) shouldBe 0
                }
                withClue("The token is created unconditionally, regardless of the life loss") {
                    game.isOnBattlefield("Faerie Rogue Token") shouldBe true
                }
            }
        }
    }
}
