package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Unwelcome Sprite ({1}{U}, 2/1 Flying Faerie Rogue).
 *
 * Whenever you cast a spell during an opponent's turn, surveil 2.
 *
 * The trigger rides on `Conditions.IsOpponentsTurn`, so it must not fire on an ally's turn in a team
 * game — that case is covered at the condition level by `IsOpponentsTurnConditionTest`.
 */
class UnwelcomeSpriteScenarioTest : ScenarioTestBase() {

    private val flashProbe = card("Flash Probe") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell {
            effect = Effects.GainLife(1)
        }
    }

    init {
        cardRegistry.register(flashProbe)

        context("Unwelcome Sprite") {

            test("surveils when you cast a spell during an opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Unwelcome Sprite")
                    .withCardInHand(1, "Flash Probe")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Flash Probe").error shouldBe null
                game.resolveStack()

                withClue("the surveil prompt is pending") {
                    game.hasPendingDecision() shouldBe true
                }
            }

            test("does not surveil when you cast a spell during your own turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Unwelcome Sprite")
                    .withCardInHand(1, "Flash Probe")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Flash Probe").error shouldBe null
                game.resolveStack()

                withClue("no trigger, so no surveil prompt") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
