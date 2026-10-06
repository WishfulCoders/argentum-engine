package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hellrider (DKA #93): "Whenever a creature you control attacks, this creature deals 1 damage to
 * the player or planeswalker it's attacking."
 *
 * Each attacking creature's trigger hits what *that* creature attacks (the 2017-03-14 ruling), via
 * `EffectTarget.AttackedPlayerOrPlaneswalker(TriggeringEntity)`: split attackers split the pings
 * between the player and the planeswalker.
 */
class HellriderScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveTriggers() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 40) {
            when (val decision = state.pendingDecision) {
                is OrderObjectsDecision -> submitDecision(OrderedResponse(decision.id, decision.objects))
                null -> passPriority().error shouldBe null
                else -> error("unexpected decision $decision")
            }
        }
    }

    init {
        test("each attacker pings the player or planeswalker it attacks") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hellrider")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(2, "Ajani Goldmane")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ajani = game.findPermanent("Ajani Goldmane")!!
            fun loyalty() = game.state.getEntity(ajani)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
            val startLoyalty = loyalty()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("Hellrider has haste, so it attacks the turn it arrives too") {
                game.declareAttackersWithPermanentTargets(
                    playerAttackers = mapOf("Hellrider" to 2, "Grizzly Bears" to 2),
                    permanentAttackers = mapOf("Hill Giant" to "Ajani Goldmane"),
                ).error shouldBe null
            }
            withClue("three attackers, three triggers") { game.state.stack.size shouldBe 3 }
            game.resolveTriggers()

            withClue("two creatures attack the opponent: 2 damage") { game.getLifeTotal(2) shouldBe 18 }
            withClue("the Giant's ping went to the planeswalker it attacks") { loyalty() shouldBe startLoyalty - 1 }
        }
    }
}
