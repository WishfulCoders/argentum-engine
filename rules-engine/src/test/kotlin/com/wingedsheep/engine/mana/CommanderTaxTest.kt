package com.wingedsheep.engine.mana

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Phase 1.4 — `CostCalculator.calculateEffectiveCost` adds {2} per prior command-zone cast when
 * `fromZone == Zone.COMMAND`, and never when the spell is being cast from any other zone.
 */
class CommanderTaxTest : FunSpec({

    val ownerId = EntityId.generate()
    val cmdrId = EntityId.generate()

    val testCommander = CardDefinition(
        name = "Test Commander",
        manaCost = ManaCost.parse("{2}{R}"),
        typeLine = TypeLine(
            supertypes = setOf(Supertype.LEGENDARY),
            cardTypes = setOf(CardType.CREATURE),
            subtypes = setOf(Subtype("Human")),
        ),
        oracleText = "",
        creatureStats = CreatureStats(2, 2),
    )

    fun stateWithCommander(castsAlreadyFromCommand: Int): GameState {
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = "Test Commander",
                name = "Test Commander",
                manaCost = ManaCost.parse("{2}{R}"),
                typeLine = testCommander.typeLine,
                oracleText = "",
                baseStats = CreatureStats(2, 2),
                colors = setOf(com.wingedsheep.sdk.core.Color.RED),
                ownerId = ownerId,
                spellEffect = null,
            ),
            OwnerComponent(ownerId),
            CommanderComponent(ownerId = ownerId, castsFromCommandZone = castsAlreadyFromCommand),
        )
        return GameState()
            .withEntity(ownerId, ComponentContainer.EMPTY)
            .withEntity(cmdrId, container)
            .addToZone(ZoneKey(ownerId, Zone.COMMAND), cmdrId)
            .copy(turnOrder = listOf(ownerId))
    }

    val registry = CardRegistry().also { it.register(testCommander) }
    val calculator = CostCalculator(registry, PredicateEvaluator(cardRegistry = null))

    test("first cast from command zone has no tax") {
        val state = stateWithCommander(castsAlreadyFromCommand = 0)
        val cost = calculator.calculateEffectiveCost(state, testCommander, ownerId, fromZone = Zone.COMMAND)
        cost.toString() shouldBe ManaCost.parse("{2}{R}").toString()
    }

    test("second cast from command zone costs {2} more") {
        val state = stateWithCommander(castsAlreadyFromCommand = 1)
        val cost = calculator.calculateEffectiveCost(state, testCommander, ownerId, fromZone = Zone.COMMAND)
        // {2}{R} + {2} = {4}{R}
        cost.cmc shouldBe 5
    }

    test("third cast from command zone costs {4} more") {
        val state = stateWithCommander(castsAlreadyFromCommand = 2)
        val cost = calculator.calculateEffectiveCost(state, testCommander, ownerId, fromZone = Zone.COMMAND)
        cost.cmc shouldBe 7
    }

    test("tax does not apply when the spell is cast from a zone other than command") {
        val state = stateWithCommander(castsAlreadyFromCommand = 5)
        val cost = calculator.calculateEffectiveCost(state, testCommander, ownerId, fromZone = Zone.HAND)
        cost.cmc shouldBe 3
    }

    test("tax does not apply when fromZone is null (legacy callers)") {
        val state = stateWithCommander(castsAlreadyFromCommand = 5)
        val cost = calculator.calculateEffectiveCost(state, testCommander, ownerId)
        cost.cmc shouldBe 3
    }
})
