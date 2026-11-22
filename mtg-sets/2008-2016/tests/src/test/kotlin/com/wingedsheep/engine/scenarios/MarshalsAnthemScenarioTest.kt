package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Marshal's Anthem (WWK #15) — {2}{W}{W} Enchantment, multikicker {1}{W}. "Creatures you control
 * get +1/+1. When this enchantment enters, return up to X target creature cards from your
 * graveyard to the battlefield, where X is the number of times this enchantment was kicked."
 *
 * The first card to read a multikicker *count*: the entry trigger's target cap is the number of
 * times the kicker was paid, not merely whether it was.
 */
class MarshalsAnthemScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Plains" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("kicked twice: returns up to two creature cards, which get +1/+1") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(8) { d.putLandOnBattlefield(player, "Plains") }
        val bears = List(3) { d.putCardInGraveyard(player, "Grizzly Bears") }
        val anthem = d.putCardInHand(player, "Marshal's Anthem")

        d.submit(
            CastSpell(player, anthem, declaredCostSlot = ChoiceSlot.KICKED, declaredCostTimes = 2)
        ).outcome shouldBe Outcome.Done
        d.bothPass() // the enchantment resolves; its entry trigger asks for targets

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.targetRequirements.single().maxTargets shouldBe 2
        d.submitTargetSelection(player, bears.take(2))
        d.bothPass()

        val onBattlefield = d.getPermanents(player).filter { d.getCardName(it) == "Grizzly Bears" }
        onBattlefield.toSet() shouldBe bears.take(2).toSet()
        d.getGraveyard(player).contains(bears[2]) shouldBe true
        onBattlefield.forEach {
            d.state.projectedState.getPower(it) shouldBe 3
            d.state.projectedState.getToughness(it) shouldBe 3
        }
    }

    test("unkicked: X is 0, nothing returns") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(4) { d.putLandOnBattlefield(player, "Plains") }
        val bear = d.putCardInGraveyard(player, "Grizzly Bears")
        val anthem = d.putCardInHand(player, "Marshal's Anthem")

        d.castSpell(player, anthem).outcome shouldBe Outcome.Done
        d.bothPass()
        if (d.state.stack.isNotEmpty()) d.bothPass()

        (d.pendingDecision is ChooseTargetsDecision) shouldBe false
        d.getGraveyard(player).contains(bear) shouldBe true
    }
})
