package com.wingedsheep.engine.stack

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Who receives priority once triggered abilities have been put on the stack (#3419).
 *
 * CR 117.5: triggered abilities are put on the stack before a player would receive priority, and
 * that player then receives it. Choosing a trigger's order, mode or targets is part of putting it on
 * the stack, not an announcement of the chooser's own — so answering it must not hand the chooser
 * priority. After the active player casts a spell, they keep it (CR 117.3c); at the start of a step,
 * the active player receives it (CR 117.3a).
 */
class PriorityAfterTriggerPlacementTest : FunSpec({

    val sorcery = card("Trigger Priority Probe Sorcery") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }
    val opponentCastWatcher = card("Trigger Priority Probe Opponent Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Whenever an opponent casts a spell, target player gains 1 life."
        triggeredAbility {
            trigger = Triggers.anOpponent.casts()
            val player = target(Targets.Player)
            effect = Effects.GainLife(1, player)
        }
    }
    val ownCastWatcher = card("Trigger Priority Probe Own Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Whenever you cast a spell, target player gains 1 life."
        triggeredAbility {
            trigger = Triggers.you.casts()
            val player = target(Targets.Player)
            effect = Effects.GainLife(1, player)
        }
    }
    val attackWatcher = card("Trigger Priority Probe Attack Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Whenever you're attacked, target player gains 1 life."
        triggeredAbility {
            trigger = Triggers.you.isAttacked()
            val player = target(Targets.Player)
            effect = Effects.GainLife(1, player)
        }
    }
    val modalWatcher = card("Trigger Priority Probe Modal Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Whenever an opponent casts a spell, choose one —\n" +
            "• Target player gains 1 life.\n• Target player gains 2 life."
        triggeredAbility {
            trigger = Triggers.anOpponent.casts()
            effect = Effects.Modal(listOf(
                Mode.withTarget(Effects.GainLife(1, EffectTarget.ContextTarget(0)), Targets.Player, "Target player gains 1 life."),
                Mode.withTarget(Effects.GainLife(2, EffectTarget.ContextTarget(0)), Targets.Player, "Target player gains 2 life."),
            ))
        }
    }
    val bear = card("Trigger Priority Probe Bear") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val probes = listOf(sorcery, opponentCastWatcher, ownCastWatcher, attackWatcher, modalWatcher, bear)

    fun twoPlayerMain(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + probes)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.answerTargets(expectedChooser: EntityId, target: EntityId) {
        val decision = pendingDecision as ChooseTargetsDecision
        decision.playerId shouldBe expectedChooser
        submitTargetSelection(expectedChooser, listOf(target)).error shouldBe null
    }

    test("the nonactive player targeting their cast trigger leaves the caster with priority") {
        val driver = twoPlayerMain()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.putPermanentOnBattlefield(opponent, opponentCastWatcher.name)

        driver.castSpell(active, driver.putCardInHand(active, sorcery.name)).error shouldBe null
        driver.priorityPlayer shouldBe active
        driver.answerTargets(opponent, opponent)

        driver.pendingDecision shouldBe null
        driver.stackSize shouldBe 2
        driver.priorityPlayer shouldBe active
    }

    test("both players targeting their cast triggers leaves the caster with priority") {
        val driver = twoPlayerMain()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.putPermanentOnBattlefield(active, ownCastWatcher.name)
        driver.putPermanentOnBattlefield(opponent, opponentCastWatcher.name)

        driver.castSpell(active, driver.putCardInHand(active, sorcery.name)).error shouldBe null
        driver.answerTargets(active, active)
        driver.answerTargets(opponent, opponent)

        driver.pendingDecision shouldBe null
        driver.stackSize shouldBe 3
        driver.priorityPlayer shouldBe active
    }

    test("the nonactive player targeting their attack trigger gives the active player priority") {
        val driver = twoPlayerMain()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.putPermanentOnBattlefield(opponent, attackWatcher.name)
        val attacker = driver.putCreatureOnBattlefield(active, bear.name)
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(attacker), defendingPlayer = opponent).error shouldBe null
        driver.answerTargets(opponent, opponent)

        driver.pendingDecision shouldBe null
        driver.stackSize shouldBe 1
        driver.priorityPlayer shouldBe active
    }

    test("the nonactive player choosing a modal trigger's mode and target leaves the caster with priority") {
        val driver = twoPlayerMain()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.putPermanentOnBattlefield(opponent, modalWatcher.name)

        driver.castSpell(active, driver.putCardInHand(active, sorcery.name)).error shouldBe null
        val mode = driver.pendingDecision as ChooseOptionDecision
        mode.playerId shouldBe opponent
        driver.submitDecision(opponent, OptionChosenResponse(mode.id, 1)).error shouldBe null
        driver.answerTargets(opponent, opponent)

        driver.pendingDecision shouldBe null
        driver.stackSize shouldBe 2
        driver.priorityPlayer shouldBe active
    }
})
