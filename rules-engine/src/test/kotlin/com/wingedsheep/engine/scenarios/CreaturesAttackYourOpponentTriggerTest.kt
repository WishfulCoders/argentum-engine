package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the `CreaturesAttackYourOpponent` trigger (Party Dude level 3): fires when one or more
 * of the controller's opponents are attacked, and NOT when the controller themself is attacked.
 */
class CreaturesAttackYourOpponentTriggerTest : FunSpec({

    val watcher = card("Test Attack Watcher") {
        manaCost = "{2}"; typeLine = "Creature — Spirit"; power = 0; toughness = 3
        triggeredAbility {
            trigger = Triggers.anOpponent.isAttacked()
            effect = Effects.GainLife(3)
        }
    }
    val bear = card("Test Bear") {
        manaCost = "{1}{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(watcher, bear))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        return d
    }

    test("fires when the controller attacks an opponent") {
        val d = createDriver()
        val me = d.player1
        val opp = d.player2

        d.removeSummoningSickness(d.putCreatureOnBattlefield(me, "Test Attack Watcher"))
        val attacker = d.putCreatureOnBattlefield(me, "Test Bear")
        d.removeSummoningSickness(attacker)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(attacker), opp)
        d.bothPass() // resolve the watcher's trigger

        d.assertLifeTotal(me, 23) // 20 + 3 from the trigger
    }

    test("does NOT fire when the controller themself is attacked") {
        val d = createDriver()
        val me = d.player1
        val opp = d.player2

        // Watcher belongs to `me`; the attacker belongs to `opp`. When opp attacks me, from the
        // watcher's perspective MY opponent (opp) is NOT attacked — I am — so it must stay silent.
        // This is the whole distinction from the "CreaturesAttackYou" trigger.
        d.removeSummoningSickness(d.putCreatureOnBattlefield(me, "Test Attack Watcher"))
        val attacker = d.putCreatureOnBattlefield(opp, "Test Bear")
        d.removeSummoningSickness(attacker)

        // Advance to opp's declare-attackers step (skip past my own combat with no attackers).
        var guard = 0
        while (!(d.state.activePlayerId == opp && d.state.step == Step.DECLARE_ATTACKERS) && guard < 500) {
            if (d.state.gameOver) throw AssertionError("Game ended before opp's declare-attackers step")
            when {
                d.state.pendingDecision != null -> d.autoResolveDecision()
                d.state.priorityPlayerId != null -> {
                    d.autoSubmitCombatDeclarationIfNeeded()
                    d.passPriority(d.state.priorityPlayerId!!)
                }
            }
            guard++
        }

        d.declareAttackers(opp, listOf(attacker), me)
        // The watcher's trigger would be on the stack here if it had fired (cf. the positive test);
        // an empty stack proves it didn't.
        d.state.stack.isEmpty() shouldBe true
        d.assertLifeTotal(me, 20)
    }
})
