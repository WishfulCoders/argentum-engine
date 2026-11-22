package com.wingedsheep.engine.targeting

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.actions.decision.DecisionValidators
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.mechanics.targeting.NumericTargetEquality
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class NumericTargetEqualityTest : FunSpec({
    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Forest" to 40))
    }
    val requirement = TargetObject(filter = TargetFilter.Creature, count = 2,
        sameNumericProperty = CardNumericProperty.TOUGHNESS)

    test("enumeration excludes buckets too small to meet a mandatory minimum") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val info = TargetEnumerationUtils(PredicateEvaluator(cardRegistry = null))
            .buildTargetInfos(d.state, d.player1, listOf(requirement)).single()
        info.validTargets.toSet() shouldBe setOf(a, b)
        info.selectionGroups shouldBe listOf(listOf(a, b))
        val three = TargetEnumerationUtils(PredicateEvaluator(cardRegistry = null))
            .buildTargetInfos(d.state, d.player1, listOf(requirement.copy(count = 3, minCount = 3))).single()
        three.validTargets shouldBe emptyList()
    }
    test("cast validator reads projected toughness including counters") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val validator = TargetValidator(PredicateEvaluator(cardRegistry = null))
        fun validate() = validator.validateTargets(d.state,
            listOf(ChosenTarget.Permanent(a), ChosenTarget.Permanent(b)), listOf(requirement), d.player1, targetingSourceType = com.wingedsheep.engine.handlers.TargetingSourceType.SPELL)
        validate().shouldNotBeNull()
        d.addComponent(a, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        validate().shouldBeNull()
    }
    test("interactive response enforces equality even without selection-group metadata") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val decision = ChooseTargetsDecision("equal", d.player1, "Choose equal toughness",
            DecisionContext(), listOf(TargetRequirementInfo(0, "two creatures", 2, 2,
                sameNumericProperty = CardNumericProperty.TOUGHNESS)), mapOf(0 to listOf(a, b)))
        val response = TargetsResponse("equal", mapOf(0 to listOf(a, b)))
        DecisionValidators.validate(decision, response, d.state).shouldNotBeNull()
        d.addComponent(a, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        DecisionValidators.validate(decision, response, d.state).shouldBeNull()
    }
    test("power, mana value and all-kind counter equality share the same axis") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        CardNumericProperty.entries.forEach { property ->
            NumericTargetEquality.isSatisfied(d.state, listOf(a, b), property) shouldBe true
        }
        d.addComponent(a, CountersComponent(mapOf(CounterType.CHARGE to 1)))
        d.addComponent(b, CountersComponent(mapOf(CounterType.QUEST to 1)))
        NumericTargetEquality.isSatisfied(d.state, listOf(a, b), CardNumericProperty.COUNTERS) shouldBe true
        d.addComponent(b, CountersComponent(mapOf(CounterType.QUEST to 2)))
        NumericTargetEquality.isSatisfied(d.state, listOf(a, b), CardNumericProperty.COUNTERS) shouldBe false
        NumericTargetEquality.isSatisfied(d.state, emptyList(), CardNumericProperty.TOUGHNESS) shouldBe true
        NumericTargetEquality.isSatisfied(d.state, listOf(a), CardNumericProperty.TOUGHNESS) shouldBe true
    }
    test("an impossible mandatory equality trigger is omitted instead of opening an unanswerable decision") {
        val d = setup()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val source = card("Equality Trigger Test") {
            manaCost = "{0}"
            typeLine = "Enchantment"
            triggeredAbility {
                trigger = Triggers.self.enters()
                targets(TargetFilter.Creature, count = 2, sameNumericProperty = CardNumericProperty.TOUGHNESS)
                effect = Effects.DrawCards(1)
            }
        }
        d.registerCards(listOf(source))
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val cardId = d.putCardInHand(d.activePlayer!!, source.name)
        d.castSpell(d.activePlayer!!, cardId).error shouldBe null
        d.bothPass()
        d.state.pendingDecision.shouldBeNull()
        d.state.stack shouldBe emptyList()
    }

    test("dependent targeting removes an opponent whose creatures cannot fill an equal-value group") {
        val d = setup()
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val requirements = listOf(
            com.wingedsheep.sdk.scripting.targets.TargetOpponent(id = "opponent"),
            requirement.copy(filter = TargetFilter(com.wingedsheep.sdk.scripting.GameObjectFilter.Creature
                .targetPlayerControls(com.wingedsheep.sdk.scripting.targets.EffectTarget.BoundVariable("opponent"))))
        )
        val infos = TargetEnumerationUtils(PredicateEvaluator(cardRegistry = null))
            .buildTargetInfos(d.state, d.player1, requirements)
        infos.first().validTargets shouldBe emptyList()
        val other = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val completable = TargetEnumerationUtils(PredicateEvaluator(cardRegistry = null))
            .buildTargetInfos(d.state, d.player1, requirements)
        completable.first().validTargets shouldBe listOf(d.player2)
        completable[1].validTargets.contains(other) shouldBe true
    }
    test("a resolution-time modal target decision enforces equality before executing its effect") {
        val d = setup()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val c = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val source = card("Equality Modal Test") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                effect = Effects.GainLife(0) then Effects.Modal(listOf(
                    com.wingedsheep.sdk.scripting.effects.Mode(effect = Effects.DrawCards(1), description = "Equal toughness",
                        targetRequirements = listOf(requirement))
                ), countsAsModalSpell = false)
            }
        }
        d.registerCards(listOf(source))
        val p = d.activePlayer!!
        d.castSpell(p, d.putCardInHand(p, source.name)).error shouldBe null
        d.bothPass()
        val mode = d.state.pendingDecision
        if (mode is ChooseModeDecision) d.submitDecision(p, ModesChosenResponse(mode.id, listOf(0))).error shouldBe null
        if (mode is ChooseOptionDecision) d.submitDecision(p, OptionChosenResponse(mode.id, 0)).error shouldBe null
        val targets = d.state.pendingDecision as ChooseTargetsDecision
        targets.targetRequirements.single().selectionGroups shouldBe listOf(listOf(a, c))
        (d.submitDecision(p, TargetsResponse(targets.id, mapOf(0 to listOf(a, b)))).outcome is Outcome.Rejected) shouldBe true
        d.submitDecision(p, TargetsResponse(targets.id, mapOf(0 to listOf(a, c)))).error shouldBe null
    }

    test("a trigger does not offer a modal choice with no compatible mandatory target group") {
        val d = setup()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val source = card("Equality Modal Trigger Test") {
            manaCost = "{0}"
            typeLine = "Enchantment"
            triggeredAbility {
                trigger = Triggers.self.enters()
                effect = Effects.Modal(listOf(com.wingedsheep.sdk.scripting.effects.Mode(
                    effect = Effects.DrawCards(1), description = "Draw for equal creatures",
                    targetRequirements = listOf(requirement))))
            }
        }
        d.registerCards(listOf(source))
        val p = d.activePlayer!!
        val before = d.getHandSize(p)
        d.castSpell(p, d.putCardInHand(p, source.name)).error shouldBe null
        d.bothPass()
        d.state.pendingDecision.shouldBeNull()
        d.state.stack shouldBe emptyList()
        d.getHandSize(p) shouldBe before
    }

})
