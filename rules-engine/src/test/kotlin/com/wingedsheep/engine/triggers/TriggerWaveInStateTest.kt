package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * The rest of a trigger wave whose placement stopped for a choice waits in
 * [GameState.triggersBeingPlaced], apart from [GameState.pendingTriggers] (CR 603.3b).
 *
 * Setup: on the active player's upkeep, two of the opponent's triggers form one wave. The first
 * targets the active player's warded creature, so the active player's ward triggers while that
 * wave is still being placed. Ward belongs to the next wave and must end up on top.
 */
class TriggerWaveInStateTest : FunSpec({

    fun upkeepTrigger(name: String) = card(name) {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
            val t = target(TargetFilter.Creature)
            effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = t)
        }
    }

    val first = upkeepTrigger("First Upkeep Elf")
    val second = upkeepTrigger("Second Upkeep Elf")
    val warded = card("Wave Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))
    }

    val json = Json {
        serializersModule = engineSerializersModule
        allowStructuredMapKeys = true
        encodeDefaults = true
    }

    /** Opponent's first upkeep trigger is asking for its target; the second waits behind it. */
    fun pausedOnFirstTrigger(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(first, second, warded))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val elfPlayer = driver.activePlayer!!
        val bearPlayer = driver.getOpponent(elfPlayer)
        driver.passPriorityUntil(Step.END)
        val bear = driver.putCreatureOnBattlefield(bearPlayer, "Wave Warded Bear")
        driver.putCreatureOnBattlefield(elfPlayer, "First Upkeep Elf")
        driver.putCreatureOnBattlefield(elfPlayer, "Second Upkeep Elf")
        // Pass into the opponent's upkeep: the elves trigger there, at the start of the bear player's turn.
        var safety = 0
        while (driver.pendingDecision == null && safety++ < 20) driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        return Triple(driver, elfPlayer, bear)
    }

    fun GameState.stackSourceNames(): List<String?> =
        stack.map { getEntity(it)?.get<TriggeredAbilityOnStackComponent>()?.sourceName }

    test("the rest of the wave is visible in the state while a trigger in it waits for targets") {
        val (driver, elfPlayer, _) = pausedOnFirstTrigger()
        driver.state.triggersBeingPlaced.map { it.controllerId } shouldContainExactly listOf(elfPlayer)
        driver.state.triggersBeingPlaced.single().sourceName shouldBe
            (if (driver.pendingDecision!!.context.sourceName == "First Upkeep Elf") "Second Upkeep Elf" else "First Upkeep Elf")
        driver.state.waitingTriggers shouldBe driver.state.triggersBeingPlaced
    }

    test("ward that triggers while a wave is being placed goes on the stack above the rest of that wave") {
        val (driver, elfPlayer, bear) = pausedOnFirstTrigger()
        val firstName = driver.pendingDecision!!.context.sourceName
        val secondName = driver.state.triggersBeingPlaced.single().sourceName

        // The first trigger targets the opponent's warded creature: ward triggers.
        driver.submitTargetSelection(elfPlayer, listOf(bear))
        // The rest of the wave is placed next and asks for its own target; ward waits for the next wave.
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.state.triggersBeingPlaced shouldBe emptyList()
        driver.state.pendingTriggers.map { it.sourceName } shouldContainExactly listOf("Wave Warded Bear")

        driver.submitTargetSelection(elfPlayer, listOf(driver.findPermanent(elfPlayer, secondName!!)!!))

        driver.state.stackSourceNames() shouldContainExactly listOf(firstName, secondName, "Wave Warded Bear")
        driver.state.waitingTriggers shouldBe emptyList()
    }

    test("a saved game with the rest of a wave on the continuation stack loads it into the state") {
        val (driver, _, _) = pausedOnFirstTrigger()
        val current = json.parseToJsonElement(json.encodeToString(GameState.serializer(), driver.state)).jsonObject
        val waiting = current.getValue("triggersBeingPlaced").jsonArray
        val stack = current.getValue("continuationStack").jsonArray
        // The former shape: the rest of the wave in a frame beneath the question.
        val legacyFrame = JsonObject(mapOf(
            "type" to JsonPrimitive("com.wingedsheep.engine.core.PendingTriggersContinuation"),
            "remainingTriggers" to waiting,
        ))
        val saved = JsonObject(current - "triggersBeingPlaced" + mapOf(
            "continuationStack" to JsonArray(stack.dropLast(1) + legacyFrame + stack.last()),
        ))

        val loaded = json.decodeFromString(GameState.serializer(), saved.toString())

        loaded shouldBe driver.state
    }
})
