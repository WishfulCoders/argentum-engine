package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.all.cards.ArcaneDenial
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Arcane Denial — "Counter target spell. Its controller may draw up to two cards at the beginning
 * of the next turn's upkeep. You draw a card at the beginning of the next turn's upkeep."
 *
 * The draws are two delayed triggers. The first names its drawer as "its controller" — the
 * countered spell's caster, fixed when the trigger is created because the spell is long gone by the
 * upkeep — and that player picks 0, 1 or 2 as it resolves (ruling).
 */
class ArcaneDenialScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ArcaneDenial))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** [caster] bolts [me]; [me] answers with Arcane Denial; both resolve. Returns the Bolt's id. */
    fun GameTestDriver.denyABolt(me: EntityId, caster: EntityId): EntityId {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        val denial = putCardInHand(me, "Arcane Denial")
        giveMana(caster, Color.RED, 1)
        giveMana(me, Color.BLUE, 2)
        passPriority(me)
        submit(CastSpell(caster, bolt, targets = listOf(ChosenTarget.Player(me)), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        passPriority(caster)
        submit(CastSpell(me, denial, targets = listOf(ChosenTarget.Spell(bolt)), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        while (stackSize > 0) bothPass()
        return bolt
    }

    /** Resolve the next upkeep's triggers; the countered spell's controller draws [chosen]. */
    fun GameTestDriver.resolveUpkeep(drawer: EntityId, chosen: Int): Boolean {
        var asked = false
        var guard = 0
        while ((stackSize > 0 || pendingDecision != null) && guard++ < 20) {
            val decision = pendingDecision
            when {
                decision is ChooseNumberDecision -> {
                    decision.playerId shouldBe drawer
                    decision.minValue shouldBe 0
                    decision.maxValue shouldBe 2
                    submitDecision(drawer, NumberChosenResponse(decision.id, chosen)).error shouldBe null
                    asked = true
                }
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
        }
        return asked
    }

    test("counters the spell; at the next upkeep its controller draws up to two and you draw one") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        val bolt = d.denyABolt(me, opp)
        d.getGraveyard(opp) shouldContain bolt
        d.getLifeTotal(me) shouldBe 20

        // Nothing is drawn yet — the triggers wait for the next turn's upkeep.
        val myHand = d.getHandSize(me)
        val theirHand = d.getHandSize(opp)
        d.passPriorityUntil(Step.UPKEEP, opp)
        d.getHandSize(me) shouldBe myHand
        d.getHandSize(opp) shouldBe theirHand

        d.resolveUpkeep(drawer = opp, chosen = 2) shouldBe true
        d.state.step shouldBe Step.UPKEEP
        d.getHandSize(opp) shouldBe theirHand + 2
        d.getHandSize(me) shouldBe myHand + 1
    }

    test("the countered spell's controller may draw nothing") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        d.denyABolt(me, opp)
        val myHand = d.getHandSize(me)
        val theirHand = d.getHandSize(opp)
        d.passPriorityUntil(Step.UPKEEP, opp)

        d.resolveUpkeep(drawer = opp, chosen = 0) shouldBe true
        d.getHandSize(opp) shouldBe theirHand
        d.getHandSize(me) shouldBe myHand + 1
    }

    test("countering your own spell: you choose up to two and also draw one") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        // The opponent casts Arcane Denial on the active player's own Bolt.
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        val denial = d.putCardInHand(opp, "Arcane Denial")
        d.giveMana(me, Color.RED, 1)
        d.giveMana(opp, Color.BLUE, 2)
        d.submit(CastSpell(me, bolt, targets = listOf(ChosenTarget.Player(opp)), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        d.passPriority(me)
        d.submit(CastSpell(opp, denial, targets = listOf(ChosenTarget.Spell(bolt)), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        while (d.stackSize > 0) d.bothPass()

        val myHand = d.getHandSize(me)
        val theirHand = d.getHandSize(opp)
        d.passPriorityUntil(Step.UPKEEP, opp)

        // The countered Bolt was the active player's, so they are the one asked.
        d.resolveUpkeep(drawer = me, chosen = 1) shouldBe true
        d.getHandSize(me) shouldBe myHand + 1
        d.getHandSize(opp) shouldBe theirHand + 1
    }
})
