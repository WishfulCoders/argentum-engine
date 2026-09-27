package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Savage Smash (RNA #203).
 *
 * "{1}{R}{G} Sorcery — Target creature you control gets +2/+2 until end of turn. It fights target
 * creature you don't control."
 *
 * Both targets are required (2019-01-25 ruling), so this is modeled with two separate mandatory
 * targets rather than one optional pair. The main scenario proves the pump lands before the fight
 * damage is calculated: a pumped 4/4 Grizzly Bears (printed 2/2) kills a 3-toughness Hill Giant
 * while itself surviving the Giant's 3 power.
 */
class SavageSmashScenarioTest : ScenarioTestBase() {

    init {
        context("Savage Smash") {

            test("pumps your creature +2/+2, then it fights and can kill without dying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Savage Smash")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 -> 4/4 after the pump
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Savage Smash"
                }
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant))
                    )
                )
                withClue("Casting Savage Smash with both required targets should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Grizzly Bears (pumped to 4/4) survives the Giant's 3 power") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Hill Giant (3 toughness) dies to 4 damage from the pumped Bears") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }

            test("cannot be cast without a legal target on both sides") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Savage Smash")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Savage Smash"
                }

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("with no creature you don't control on the battlefield, the second target can't be filled") {
                    (cast.error != null) shouldBe true
                }
            }
        }
    }
}
