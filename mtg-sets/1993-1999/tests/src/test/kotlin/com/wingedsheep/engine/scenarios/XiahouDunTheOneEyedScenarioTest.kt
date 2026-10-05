package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class XiahouDunTheOneEyedScenarioTest : ScenarioTestBase() {
    private fun abilityId() = cardRegistry.getCard("Xiahou Dun, the One-Eyed")!!.script.activatedAbilities[0].id

    init {
        context("Xiahou Dun, the One-Eyed") {
            test("sacrifice to return a black card from my graveyard to hand") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Xiahou Dun, the One-Eyed", summoningSickness = false)
                    .withCardInGraveyard(1, "Cao Cao, Lord of Wei")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val xd = game.findPermanent("Xiahou Dun, the One-Eyed")!!
                val target = game.findCardsInGraveyard(1, "Cao Cao, Lord of Wei").single()
                game.execute(
                    ActivateAbility(game.player1Id, xd, abilityId(), targets = listOf(ChosenTarget.Card(target, game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD)))
                ).error shouldBe null
                game.resolveStack()
                game.isInHand(1, "Cao Cao, Lord of Wei") shouldBe true
                game.isInGraveyard(1, "Xiahou Dun, the One-Eyed") shouldBe true
            }

            test("cannot target a non-black card") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Xiahou Dun, the One-Eyed", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val xd = game.findPermanent("Xiahou Dun, the One-Eyed")!!
                val target = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.execute(
                    ActivateAbility(game.player1Id, xd, abilityId(), targets = listOf(ChosenTarget.Card(target, game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD)))
                ).error shouldNotBe null
            }
        }
    }
}
