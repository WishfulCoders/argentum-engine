package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * The remover axis of `EventPattern.CountersRemovedEvent` — `removedBy`, the mirror of
 * `CountersPlacedEvent.placedBy`. "When **you** remove the last intervention counter" (Divine
 * Intervention) fires only when the trigger's controller removed it; a removal by an opponent, or
 * one no player made (a state-based action, damage to a battle), never matches.
 */
class CountersRemovedByPlayerAxisTest : FunSpec({

    val youRemoveLast = card("You Remove Last Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.losesCounters(CounterType.CHARGE, lastRemoved = true, by = Player.You)
            effect = Effects.DrawCards(1)
        }
        activatedAbility {
            cost = Costs.RemoveCounterFromSelf(CounterType.CHARGE)
            effect = Effects.GainLife(1)
        }
    }

    val anyoneRemoves = card("Anyone Removes Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.losesCounters(CounterType.CHARGE)
            effect = Effects.DrawCards(1)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(youRemoveLast, anyoneRemoves))
        initMirrorMatch(deck = Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.triggersOf(sourceId: EntityId, vararg events: CountersRemovedEvent) =
        TriggerDetector(cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null), conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions)
            .detectTriggers(state, events.toList())
            .filter { it.ability.trigger is EventPattern.CountersRemovedEvent && it.sourceId == sourceId }

    fun removal(entityId: EntityId, removedBy: EntityId?, remaining: Int = 0) =
        CountersRemovedEvent(entityId, CounterType.CHARGE, 1, remainingCount = remaining, removedBy = removedBy)

    test("fires when its controller removed the last counter") {
        val d = driver()
        val observer = d.putCreatureOnBattlefield(d.player1, "You Remove Last Observer")
        d.triggersOf(observer, removal(observer, d.player1)) shouldHaveSize 1
    }

    test("doesn't fire when an opponent removed it") {
        val d = driver()
        val observer = d.putCreatureOnBattlefield(d.player1, "You Remove Last Observer")
        d.triggersOf(observer, removal(observer, d.player2)) shouldHaveSize 0
    }

    test("an unattributed removal never matches a remover-restricted trigger, but does an open one") {
        val d = driver()
        val restricted = d.putCreatureOnBattlefield(d.player1, "You Remove Last Observer")
        val open = d.putCreatureOnBattlefield(d.player1, "Anyone Removes Observer")
        d.triggersOf(restricted, removal(restricted, null)) shouldHaveSize 0
        d.triggersOf(open, removal(open, null)) shouldHaveSize 1
        d.triggersOf(open, removal(open, d.player2)) shouldHaveSize 1
    }

    test("the remover axis composes with lastRemoved: removing a counter that isn't the last is silent") {
        val d = driver()
        val observer = d.putCreatureOnBattlefield(d.player1, "You Remove Last Observer")
        d.triggersOf(observer, removal(observer, d.player1, remaining = 1)) shouldHaveSize 0
    }

    test("a remove-counter cost is attributed to the player who paid it") {
        val d = driver()
        val observer = d.putCreatureOnBattlefield(d.player1, "You Remove Last Observer")
        d.replaceState(d.state.updateEntity(observer) { it.with(CountersComponent().withAdded(CounterType.CHARGE, 1)) })

        val result = d.submit(
            ActivateAbility(
                playerId = d.player1,
                sourceId = observer,
                abilityId = youRemoveLast.activatedAbilities.single().id,
            )
        )
        result.outcome shouldBe Outcome.Done
        result.events.filterIsInstance<CountersRemovedEvent>().single().removedBy shouldBe d.player1
        // The last charge counter came off by its controller's hand: the draw trigger is waiting.
        d.state.stack.size shouldBe 2
    }
})
