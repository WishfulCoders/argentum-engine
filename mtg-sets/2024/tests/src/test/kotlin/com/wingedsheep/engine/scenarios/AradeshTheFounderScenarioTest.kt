package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.AradeshTheFounder
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Enlist
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AradeshTheFounderScenarioTest : FunSpec({
    val other = card("Aradesh Other Enlister") {
        typeLine = "Creature — Soldier"; power = 2; toughness = 4
        staticAbility { ability = Enlist }
    }
    fun instant(name: String, make: (com.wingedsheep.sdk.scripting.targets.EffectTarget) -> com.wingedsheep.sdk.scripting.effects.Effect) = card(name) {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val target = target(TargetFilter.Creature); effect = make(target) }
    }
    val kill = instant("Aradesh Test Kill") { Effects.Destroy(it) }
    val blink = instant("Aradesh Test Blink") { Effects.Exile(it) then Effects.PutOntoBattlefield(it) }
    val remove = instant("Aradesh Test Remove From Combat") { Effects.RemoveFromCombat(it) }
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(AradeshTheFounder, other, kill, blink, remove))
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun ready(d: GameTestDriver, name: String, player: EntityId = d.player1) =
        d.putCreatureOnBattlefield(player, name).also(d::removeSummoningSickness)
    fun order(d: GameTestDriver) {
        val q = d.pendingDecision as? OrderObjectsDecision ?: return
        d.submitOrderedResponse(q.playerId, q.objects).error shouldBe null
    }
    fun attack(d: GameTestDriver, attacker: EntityId, helper: EntityId?) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
        d.submitCardSelection(d.player1, listOfNotNull(helper)).error shouldBe null
        order(d)
    }
    // Exercise both legal simultaneous-trigger stack orders without depending on the default order.
    fun stackOrder(d: GameTestDriver, boostFirst: Boolean) {
        d.state.stack.size shouldBe 2
        val boost = d.state.stack.single {
            d.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()?.effect is ModifyStatsEffect
        }
        val payoff = d.state.stack.single { it != boost }
        d.replaceState(d.state.copy(stack = if (boostFirst) listOf(payoff, boost) else listOf(boost, payoff)))
    }
    fun resolve(d: GameTestDriver) {
        order(d)
        repeat(8) { if (d.state.stack.isNotEmpty()) d.bothPass() }
        d.state.stack.size shouldBe 0
    }
    fun cast(d: GameTestDriver, name: String, target: EntityId) {
        val player = d.state.priorityPlayerId!!
        d.castSpell(player, d.putCardInHand(player, name), listOf(target)).error shouldBe null
        d.bothPass()
    }
    fun doubleStrike(d: GameTestDriver, id: EntityId) = d.state.projectedState.hasKeyword(id, Keyword.DOUBLE_STRIKE)

    test("Aradesh enlists, gains double strike and draws after its boost reaches four power") {
        val d = setup(); val a = ready(d, AradeshTheFounder.name); val helper = ready(d, "Centaur Courser")
        val hand = d.getHandSize(d.player1)
        attack(d, a, helper); stackOrder(d, true); resolve(d)
        d.state.projectedState.getPower(a) shouldBe 4
        doubleStrike(d, a) shouldBe true
        d.getHandSize(d.player1) shouldBe hand + 1
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.state.enlistmentsThisCombat shouldBe emptyList()
        d.passPriorityUntil(Step.UPKEEP)
        doubleStrike(d, a) shouldBe false
    }

    test("draw checks power at resolution so resolving Aradesh before the boost draws nothing") {
        val d = setup(); val a = ready(d, AradeshTheFounder.name); val helper = ready(d, "Centaur Courser")
        val hand = d.getHandSize(d.player1)
        attack(d, a, helper); stackOrder(d, false); resolve(d)
        doubleStrike(d, a) shouldBe true
        d.state.projectedState.getPower(a) shouldBe 4
        d.getHandSize(d.player1) shouldBe hand
    }

    test("enlisting below four power still grants double strike without a draw") {
        val d = setup(); val a = ready(d, AradeshTheFounder.name); val helper = ready(d, "Grizzly Bears")
        val hand = d.getHandSize(d.player1)
        attack(d, a, helper); stackOrder(d, true); resolve(d)
        doubleStrike(d, a) shouldBe true
        d.getHandSize(d.player1) shouldBe hand
    }

    test("declining enlist produces no Aradesh attack trigger") {
        val d = setup(); val a = ready(d, AradeshTheFounder.name); ready(d, "Centaur Courser")
        attack(d, a, null)
        d.state.stack.size shouldBe 0
        doubleStrike(d, a) shouldBe false
    }

    test("Aradesh observes another enlisting attacker and its trigger survives Aradesh leaving") {
        val d = setup(); val aradesh = ready(d, AradeshTheFounder.name)
        val a = ready(d, other.name); val helper = ready(d, "Centaur Courser")
        attack(d, a, helper); stackOrder(d, true)
        val hand = d.getHandSize(d.player1)
        cast(d, kill.name, aradesh); resolve(d)
        doubleStrike(d, a) shouldBe true
        d.getHandSize(d.player1) shouldBe hand + 1
    }

    test("the enlisted helper leaving does not erase the attacker's enlist history") {
        val d = setup(); val a = ready(d, AradeshTheFounder.name); val helper = ready(d, "Centaur Courser")
        attack(d, a, helper); stackOrder(d, true)
        val hand = d.getHandSize(d.player1)
        cast(d, kill.name, helper); resolve(d)
        doubleStrike(d, a) shouldBe true
        d.getHandSize(d.player1) shouldBe hand + 1
    }

    test("removing an attacker from combat does not erase its enlist history") {
        val d = setup(); val a = ready(d, AradeshTheFounder.name); val helper = ready(d, "Centaur Courser")
        attack(d, a, helper); stackOrder(d, true)
        val hand = d.getHandSize(d.player1)
        cast(d, remove.name, a); resolve(d)
        doubleStrike(d, a) shouldBe true
        d.getHandSize(d.player1) shouldBe hand + 1
    }

    for (departure in listOf(kill, blink)) {
        test("a ${if (departure == blink) "blinked" else "dead"} attacker uses its departed power for the draw") {
            val d = setup(); val a = ready(d, AradeshTheFounder.name); val helper = ready(d, "Centaur Courser")
            attack(d, a, helper); stackOrder(d, true)
            d.bothPass() // The enlist boost resolves; Aradesh's trigger remains on the stack.
            d.state.projectedState.getPower(a) shouldBe 4
            val hand = d.getHandSize(d.player1)
            cast(d, departure.name, a); resolve(d)
            d.getHandSize(d.player1) shouldBe hand + 1
            if (departure == blink) {
                d.state.projectedState.getPower(a) shouldBe 1
                doubleStrike(d, a) shouldBe false
            }
        }
    }

    test("an opposing Aradesh does not trigger for your enlisting attacker") {
        val d = setup(); ready(d, AradeshTheFounder.name, d.player2)
        val a = ready(d, other.name); val helper = ready(d, "Centaur Courser")
        attack(d, a, helper)
        d.state.stack.size shouldBe 1
        resolve(d); doubleStrike(d, a) shouldBe false
    }
})
