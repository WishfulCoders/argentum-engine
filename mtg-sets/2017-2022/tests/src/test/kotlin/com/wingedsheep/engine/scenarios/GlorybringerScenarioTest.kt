package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.Glorybringer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Glorybringer (AKH #134) — {3}{R}{R} Creature — Dragon 4/4.
 *
 *   Flying, haste
 *   You may exert this creature as it attacks. When you do, it deals 4 damage to target non-Dragon
 *   creature an opponent controls.
 */
class GlorybringerScenarioTest : FunSpec({

    val wyrm = card("Test Glory Wyrm") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Dragon"
        power = 1
        toughness = 1
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(Glorybringer, wyrm))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("exerting deals 4 damage to a non-Dragon creature an opponent controls") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val glory = d.putCreatureOnBattlefield(me, "Glorybringer") // haste: no sickness removal needed
        val mine = d.putCreatureOnBattlefield(me, "Centaur Courser")
        val courser = d.putCreatureOnBattlefield(opp, "Centaur Courser")
        val dragon = d.putCreatureOnBattlefield(opp, "Test Glory Wyrm")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(glory), opp)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, listOf(glory))
        d.state.getEntity(glory)?.has<ExertedComponent>() shouldBe true

        val choose = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        choose.legalTargets.values.flatten() shouldContainExactlyInAnyOrder listOf(courser)
        d.submitTargetSelection(me, listOf(courser))
        d.bothPass()

        d.state.getBattlefield(opp).contains(courser) shouldBe false
        d.state.getBattlefield(opp).contains(dragon) shouldBe true
        d.state.getBattlefield(me).contains(mine) shouldBe true
    }

    test("not exerting: no trigger, Glorybringer is not exerted") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val glory = d.putCreatureOnBattlefield(me, "Glorybringer")
        d.putCreatureOnBattlefield(opp, "Centaur Courser")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(glory), opp)
        d.submitCardSelection(me, emptyList())
        d.state.stack.isEmpty() shouldBe true
        d.state.getEntity(glory)?.has<ExertedComponent>() shouldBe false
    }
})
