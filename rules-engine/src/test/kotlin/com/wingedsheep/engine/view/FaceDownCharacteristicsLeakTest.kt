package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.FACE_DOWN_DISPLAY_NAME
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

/**
 * Three ways the card under a face-down permanent reached its opponent, all found by
 * `HiddenIdentityRandomGamesTest` (issue #2780) and each pinned here on its own.
 *
 * A face-down permanent is a nameless, colourless 2/2 with no abilities (CR 708.2a); nothing about
 * the card underneath may show until it is turned face up or leaves the battlefield.
 */
class FaceDownCharacteristicsLeakTest : FunSpec({

    // "At the beginning of your upkeep, if you control three or more creatures, draw a card." —
    // an intervening-if the client badges with its progress ("1/3").
    val counter = card("Test Creature Counter") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elf"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            interveningIf = Conditions.YouControlAtLeast(3, GameObjectFilter.Creature)
            effect = Effects.DrawCards(1)
        }
    }

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(counter))
        initMirrorMatch(deck = Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.faceDown(entityId: EntityId) =
        replaceState(state.updateEntity(entityId) { it.with(FaceDownComponent) })

    fun GameTestDriver.visibility() =
        Visibility(cardRegistry, conditionEvaluator = PredicateEvaluator(cardRegistry = cardRegistry).conditions)

    test("an event about a face-down permanent does not name it to the opponent") {
        val driver = newDriver()
        val controller = driver.player1
        val opponent = driver.player2
        val hidden = driver.putCreatureOnBattlefield(controller, "Savannah Lions")
        driver.faceDown(hidden)
        // Engine events copy the name at emission, as every counter-placing executor does.
        val event = CountersAddedEvent(hidden, CounterType.PLUS_ONE_PLUS_ONE, 1, entityName = "Savannah Lions")

        val seenByOpponent = ClientEventTransformer.transform(listOf(event), opponent, driver.state, driver.visibility())
            .single() as ClientEvent.CounterAdded
        seenByOpponent.permanentName shouldBe FACE_DOWN_DISPLAY_NAME
        seenByOpponent.description shouldNotContain "Savannah Lions"

        // CR 708.5: its controller may look at it.
        val seenByController = ClientEventTransformer.transform(listOf(event), controller, driver.state, driver.visibility())
            .single() as ClientEvent.CounterAdded
        seenByController.permanentName shouldBe "Savannah Lions"
    }

    test("a face-down permanent shows no badge for its card's triggered ability") {
        val driver = newDriver()
        val hidden = driver.putCreatureOnBattlefield(driver.player1, "Test Creature Counter")
        val transformer = ClientStateTransformer(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = driver.cardRegistry))
        fun badges(viewer: EntityId) = transformer.transform(driver.state, viewer).cards.getValue(hidden)
            .activeEffects.filter { it.effectId.startsWith("condition_compare") }

        badges(driver.player1).map { it.name } shouldBe listOf("1/3")

        driver.faceDown(hidden)
        badges(driver.player2).shouldBeEmpty()
        // Not even for its controller: a face-down permanent has no abilities to make progress on.
        badges(driver.player1).shouldBeEmpty()
    }

    test("a face-down permanent adds no colour to the colours among permanents you control") {
        val driver = newDriver()
        val hidden = driver.putCreatureOnBattlefield(driver.player1, "Savannah Lions")
        val colors = DynamicAmounts.colorsAmongPermanents()
        fun count() = driver.services.predicateEvaluator.amounts
            .evaluate(driver.state, colors, EffectContext(sourceId = null, controllerId = driver.player1))

        count() shouldBe 1

        driver.faceDown(hidden)
        count() shouldBe 0
    }
})
