package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Adeline, Resplendent Cathar (MID #1).
 *
 * The attack trigger is `CreateToken(attackingEach = Player.EachOpponent)`: a token per opponent,
 * tapped and attacking that opponent — or, chosen as it enters, a planeswalker they control
 * (CR 508.4). Attacking with any creature triggers it, Adeline among the attackers or not.
 */
class AdelineResplendentCatharScenarioTest : ScenarioTestBase() {

    private fun board(vararg opponent: String): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Adeline, Resplendent Cathar", summoningSickness = false)
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        opponent.forEach { builder.withCardOnBattlefield(2, it) }
        return builder.build()
    }

    init {
        test("power counts the creatures you control, Adeline included") {
            val game = board()
            game.state.projectedState.getPower(game.findPermanent("Adeline, Resplendent Cathar")!!) shouldBe 2
        }

        test("attacking without Adeline still makes a tapped Human attacking the opponent") {
            val game = board()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            withClue("the opponent controls no planeswalker, so there is nothing to choose") {
                game.hasPendingDecision() shouldBe false
            }
            val human = game.findAllPermanents("Human Token").single()
            game.state.getEntity(human)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(human)!!.get<AttackingComponent>()!!.defenderId shouldBe game.player2Id
            withClue("Adeline now counts three creatures") {
                game.state.projectedState.getPower(game.findPermanent("Adeline, Resplendent Cathar")!!) shouldBe 3
            }
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("Bears 2 + Human 1") { game.getLifeTotal(2) shouldBe 17 }
        }

        test("the opponent's planeswalker may be the token's defender") {
            val game = board("Ajani Goldmane")
            val ajani = game.findPermanent("Ajani Goldmane")!!
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Adeline, Resplendent Cathar" to 2)).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.playerId shouldBe game.player1Id
            decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(game.player2Id, ajani)
            game.selectTargets(listOf(ajani)).error shouldBe null

            val human = game.findAllPermanents("Human Token").single()
            game.state.getEntity(human)!!.get<AttackingComponent>()!!.defenderId shouldBe ajani
            withClue("Adeline has vigilance") {
                game.state.getEntity(game.findPermanent("Adeline, Resplendent Cathar")!!)!!.has<TappedComponent>() shouldBe false
            }
        }
    }
}
