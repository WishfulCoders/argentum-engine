package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Targets
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Who draws, when the drawing player is named through the resolving spell's targets.
 *
 * Two engine gaps, both silent no-ops before:
 *  - `DrawUpToExecutor` (and a dozen sibling executors) resolved its player with a *stateless*
 *    resolver that knew neither [EffectTarget.TargetController] ("its controller") nor a player
 *    pinned by `SpecificEntity`. The stateless overload is gone; every executor reads the state.
 *  - `CreateDelayedTriggerExecutor` didn't fix a draw's player to a concrete id when it scheduled
 *    the trigger, so a draw named by a target ("target player", "its controller") had nothing to
 *    resolve against by the time the trigger fired (CR 603.7c: a delayed trigger still refers to
 *    the objects its creating effect named).
 */
class DelayedDrawRecipientTest : FunSpec({

    // "Counter target spell. Its controller may draw up to two cards." — no delay: the stateless gap.
    val denyAndRefund = card("Deny and Refund") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Counter target spell. Its controller may draw up to two cards."
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.DrawUpTo(2, EffectTarget.TargetController) then Effects.CounterSpell()
        }
    }

    // "Target player draws two cards at the beginning of the next turn's upkeep." — the baking gap.
    val slowGift = card("Slow Gift") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Target player draws two cards at the beginning of the next turn's upkeep."
        spell {
            target(Targets.Player)
            effect = Effects.CreateDelayedTrigger(
                step = Step.UPKEEP,
                timing = DelayedTriggerTiming.NEXT_TURN,
                effect = Effects.DrawCards(2, EffectTarget.ContextTarget(0))
            )
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(denyAndRefund, slowGift))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("'its controller may draw up to two' asks the countered spell's caster, and they draw") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.putCardInHand(me, "Grizzly Bears")
        val deny = d.putCardInHand(opp, "Deny and Refund")
        d.giveMana(me, com.wingedsheep.sdk.core.Color.GREEN, 2)
        d.castSpell(me, bears).error shouldBe null
        d.passPriority(me)
        d.submit(CastSpell(opp, deny, targets = listOf(ChosenTarget.Spell(bears)), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        val handBefore = d.getHandSize(me)
        d.bothPass()

        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        choice.playerId shouldBe me
        d.submitDecision(me, NumberChosenResponse(choice.id, 2)).error shouldBe null
        d.getHandSize(me) shouldBe handBefore + 2
    }

    test("a delayed draw named by 'target player' still draws for that player when it fires") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val gift = d.putCardInHand(me, "Slow Gift")
        d.castSpell(me, gift, targets = listOf(opp)).error shouldBe null
        d.bothPass()

        val theirHand = d.getHandSize(opp)
        val myHand = d.getHandSize(me)
        d.passPriorityUntil(Step.UPKEEP, opp)
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()

        d.state.step shouldBe Step.UPKEEP
        d.getHandSize(opp) shouldBe theirHand + 2
        d.getHandSize(me) shouldBe myHand
    }
})
