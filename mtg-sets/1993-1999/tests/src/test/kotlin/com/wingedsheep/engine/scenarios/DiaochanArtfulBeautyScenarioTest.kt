package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DiaochanArtfulBeautyScenarioTest : ScenarioTestBase() {
    private fun abilityId() = cardRegistry.getCard("Diaochan, Artful Beauty")!!.script.activatedAbilities[0].id

    init {
        context("Diaochan, Artful Beauty") {
            test("destroys my chosen creature, then the creature the opponent chooses") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Diaochan, Artful Beauty", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dia = game.findPermanent("Diaochan, Artful Beauty")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.execute(
                    ActivateAbility(game.player1Id, dia, abilityId(), targets = listOf(ChosenTarget.Permanent(giant)))
                ).error shouldBe null

                val d = game.getPendingDecision()
                d.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("the opponent makes the second choice") { d.playerId shouldBe game.player2Id }
                d.legalTargets[0]!! shouldContain dia
                // The opponent picks Diaochan herself.
                game.selectTargets(listOf(dia))
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isOnBattlefield("Diaochan, Artful Beauty") shouldBe false
                game.findAllPermanents("Grizzly Bears").size shouldBe 2
            }

            test("cannot be activated after attackers are declared") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Diaochan, Artful Beauty", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val dia = game.findPermanent("Diaochan, Artful Beauty")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.execute(
                    ActivateAbility(game.player1Id, dia, abilityId(), targets = listOf(ChosenTarget.Permanent(giant)))
                ).error shouldNotBe null
            }
        }
    }
}
