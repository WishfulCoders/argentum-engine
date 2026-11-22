package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.Enlist
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EnlistCombatHistoryTest : FunSpec({
    val double = card("Combat History Enlister") {
        typeLine = "Creature — Soldier"; power = 1; toughness = 4
        repeat(2) { staticAbility { ability = Enlist } }
    }
    val tax = card("Combat History Tax") {
        typeLine = "Enchantment"; staticAbility { ability = AttackTax(DynamicAmount.Fixed(1)) }
    }
    val blink = card("Combat History Blink") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val target = target(TargetFilter.Creature); effect = Effects.Exile(target) then Effects.PutOntoBattlefield(target) }
    }
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(double, tax, blink))
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun ready(d: GameTestDriver, name: String) = d.putCreatureOnBattlefield(d.player1, name).also(d::removeSummoningSickness)

    test("paid instances are counted separately before boost triggers resolve and serialize") {
        val d = setup(); val attacker = ready(d, double.name)
        val helpers = listOf(ready(d, "Grizzly Bears"), ready(d, "Centaur Courser"))
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
        for (helper in helpers) d.submitCardSelection(d.player1, listOf(helper)).error shouldBe null
        val reference = d.state.objectRef(attacker)!!
        d.state.enlistmentsThisCombat.count { it == reference } shouldBe 2
        val amounts = PredicateEvaluator(d.cardRegistry).conditions.amounts
        val context = EffectContext(attacker, d.player1)
        amounts.evaluate(d.state, DynamicAmounts.enlistmentsThisCombat(EffectTarget.Self), context) shouldBe 2
        amounts.evaluate(d.state, DynamicAmounts.enlistmentsThisCombat(EffectTarget.ContextTarget(0)),
            context.copy(targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(helpers[0])))) shouldBe 0
        val json = kotlinx.serialization.json.Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        val restored = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), d.state))
        restored.enlistmentsThisCombat shouldBe d.state.enlistmentsThisCombat
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.state.enlistmentsThisCombat shouldBe emptyList()
    }

    test("declined enlist and cancelled attack taxes leave no combat history") {
        for (cancel in listOf(false, true)) {
            val d = setup(); val attacker = ready(d, double.name); val helper = ready(d, "Grizzly Bears")
            if (cancel) d.putPermanentOnBattlefield(d.player2, tax.name)
            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
            d.submitCardSelection(d.player1, if (cancel) listOf(helper) else emptyList()).error shouldBe null
            if (d.pendingDecision is SelectCardsDecision) d.submitCardSelection(d.player1, emptyList()).error shouldBe null
            if (cancel) d.submitManaAutoPayOrDecline(d.player1, false).error shouldBe null
            d.state.enlistmentsThisCombat shouldBe emptyList()
        }
    }

    test("a captured departed object keeps its history while a returned object has none") {
        val d = setup(); val attacker = ready(d, double.name); val helper = ready(d, "Grizzly Bears")
        val paid = EnlistAttackCosts.pay(d.state, listOf(EnlistPayment(attacker, helper)))
        paid.error shouldBe null; d.replaceState(paid.newState)
        val original = d.state.objectRef(attacker)!!
        val spell = d.putCardInHand(d.player1, blink.name)
        d.castSpell(d.player1, spell, listOf(attacker)).error shouldBe null; d.bothPass()
        val amounts = PredicateEvaluator(d.cardRegistry).conditions.amounts
        val amount = DynamicAmounts.enlistmentsThisCombat(EffectTarget.Self)
        amounts.evaluate(d.state, amount, EffectContext(attacker, d.player1)) shouldBe 0
        val captured = EffectContext(attacker, d.player1, objectReferences = ObjectReferenceEnvironment(
            captured = true, origin = original, source = original, triggering = original), triggeringEntityId = attacker)
        amounts.evaluate(d.state, amount, captured) shouldBe 1
        amounts.evaluate(d.state, DynamicAmounts.enlistmentsThisCombat(EffectTarget.TriggeringEntity), captured) shouldBe 1
    }

    test("an invalid later payment rolls back earlier history and taps atomically") {
        val d = setup(); val attacker = ready(d, double.name); val helper = ready(d, "Grizzly Bears")
        val before = d.state
        val result = EnlistAttackCosts.pay(before, listOf(EnlistPayment(attacker, helper), EnlistPayment(attacker, helper)))
        (result.error != null) shouldBe true
        result.newState shouldBe before
        result.newState.enlistmentsThisCombat shouldBe emptyList()
    }
})
