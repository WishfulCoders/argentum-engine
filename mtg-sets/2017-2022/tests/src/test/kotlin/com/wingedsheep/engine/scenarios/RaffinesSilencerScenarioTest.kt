package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Raffine's Silencer (SNC #90) — {2}{B} Creature — Human Assassin 1/1
 *
 *   When this creature enters, it connives.
 *   When this creature dies, target creature an opponent controls gets -X/-X until end of turn,
 *   where X is this creature's power.
 *
 * X is the Silencer's power as it last existed on the battlefield (ruling 2022-04-29): the
 * +1/+1 counter it got from conniving still counts once it is in the graveyard.
 */
class RaffinesSilencerScenarioTest : ScenarioTestBase() {

    init {
        context("Raffine's Silencer") {

            test("dying shrinks the target by its last-known power, connive counter included") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Raffine's Silencer")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bearsInHand = game.findCardsInHand(1, "Grizzly Bears").single()

                game.castSpell(1, "Raffine's Silencer").error shouldBe null
                game.resolveStack()

                withClue("the ETB connive pauses on the discard") {
                    game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                }
                game.selectCards(listOf(bearsInHand)).error shouldBe null
                game.resolveStack()

                val silencer = game.findPermanent("Raffine's Silencer")!!
                withClue("discarding a nonland card grew the Silencer to 2/2") {
                    game.state.projectedState.getPower(silencer) shouldBe 2
                }

                game.castSpell(1, "Murder", silencer).error shouldBe null
                game.resolveStack()

                withClue("the Silencer died") { game.isInGraveyard(1, "Raffine's Silencer") shouldBe true }
                withClue("its dies trigger asks for a creature an opponent controls") {
                    game.selectTargets(listOf(giant)).error shouldBe null
                }
                game.resolveStack()

                withClue("Hill Giant got -2/-2 from the Silencer's last-known power") {
                    game.state.projectedState.getPower(giant) shouldBe 1
                    game.state.projectedState.getToughness(giant) shouldBe 1
                }
            }
        }
    }
}
