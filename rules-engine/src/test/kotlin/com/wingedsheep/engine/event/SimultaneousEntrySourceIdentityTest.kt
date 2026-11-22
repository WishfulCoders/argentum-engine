package com.wingedsheep.engine.event

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SimultaneousEntrySourceIdentityTest : FunSpec({
    val observer = card("Simultaneous Entry Identity Observer") {
        typeLine = "Creature — Human"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
            effect = Effects.AddCounters(CounterType.QUEST, 1, com.wingedsheep.sdk.scripting.targets.EffectTarget.Self)
        }
    }
    for (sourceFirst in listOf(false, true)) {
        test("entry observer binds its battlefield visit when emitted ${if (sourceFirst) "first" else "last"}") {
            val d = GameTestDriver().apply {
                registerCards(TestCards.all + observer)
                initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            val source = d.putCardInHand(d.player1, observer.name)
            val entrant = d.putCardInHand(d.player1, "Grizzly Bears")
            val oldSource = d.state.objectRef(source)
            val moved = d.services.zones.moveToZoneBatch(d.state,
                if (sourceFirst) listOf(source, entrant) else listOf(entrant, source), Zone.BATTLEFIELD)
            val pending = d.services.triggerDetector.detectTriggers(moved.state, moved.events)
                .single { it.sourceId == source }
            pending.objectReferences.source shouldBe moved.state.objectRef(source)
            pending.objectReferences.source shouldNotBe oldSource
            pending.objectReferences.triggering shouldBe moved.state.objectRef(entrant)
        }
    }
    test("an observer entering in a later Composite instruction cannot observe the earlier entry") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + observer)
            initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val source = d.putCardInHand(d.player1, observer.name)
        val entrant = d.putCardInHand(d.player1, "Grizzly Bears")
        val effect = Effects.Composite(listOf(
            Effects.PutOntoBattlefield(com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(entrant)),
            Effects.PutOntoBattlefield(com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(source))
        ))
        val result = d.services.effectExecutorRegistry.execute(d.state, effect,
            com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = d.player1))
        result.error shouldBe null
        (source in result.state.getBattlefield()) shouldBe true
        (entrant in result.state.getBattlefield()) shouldBe true
        val entries = result.events.filterIsInstance<com.wingedsheep.engine.core.ZoneChangeEvent>()
            .filter { it.toZone == Zone.BATTLEFIELD }
        entries.size shouldBe 2
        d.services.triggerDetector.detectTriggers(result.state, result.events)
            .filter { it.sourceId == source }.size shouldBe 0
        (result.state.getEntity(source)?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
            ?.getCount(CounterType.QUEST) ?: 0) shouldBe 0
    }

    test("several token copies in one instruction share an entry group and bind their own new objects") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + observer)
            initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val original = d.putCreatureOnBattlefield(d.player1, observer.name)
        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.CreateTokenCopyOfTarget(
                com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(original), count = 2),
            com.wingedsheep.engine.handlers.EffectContext(sourceId = original, controllerId = d.player1))
        result.error shouldBe null
        val entries = result.events.filterIsInstance<com.wingedsheep.engine.core.ZoneChangeEvent>()
            .filter { it.toZone == Zone.BATTLEFIELD }
        entries.size shouldBe 2
        (entries.first().simultaneousEntryGroup != null) shouldBe true
        entries.map { it.simultaneousEntryGroup }.distinct().size shouldBe 1
        val pending = d.services.triggerDetector.detectTriggers(result.state, result.events)
        for (entry in entries) {
            val own = pending.single { it.sourceId == entry.entityId }
            own.objectReferences.source shouldBe result.state.objectRef(entry.entityId)
        }
    }

    for (together in listOf(false, true)) {
        test("mixed token groups are ${if (together) "simultaneous in CreateTokensTogether" else "sequential in Composite"}") {
            val d = GameTestDriver().apply {
                registerCards(TestCards.all + observer)
                initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            val earlier = Effects.CreateToken(power = 1, toughness = 1,
                creatureTypes = setOf("Goblin"), name = "Entry Test Goblin")
            val later = Effects.CreateToken(power = 2, toughness = 2,
                creatureTypes = setOf("Human"), name = "Entry Test Observer Token",
                triggeredAbilities = observer.triggeredAbilities)
            val effect = if (together) Effects.CreateTokensTogether(earlier, later)
                else Effects.Composite(listOf(earlier, later))
            val result = d.services.effectExecutorRegistry.execute(d.state, effect,
                com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = d.player1))
            result.error shouldBe null
            val entries = result.events.filterIsInstance<com.wingedsheep.engine.core.ZoneChangeEvent>()
                .filter { it.toZone == Zone.BATTLEFIELD }
            entries.size shouldBe 2
            val source = entries.single { it.entityName == "Entry Test Observer Token" }.entityId
            val own = d.services.triggerDetector.detectTriggers(result.state, result.events)
                .filter { it.sourceId == source }
            own.size shouldBe if (together) 1 else 0
            if (together) {
                (entries.first().simultaneousEntryGroup != null) shouldBe true
                entries.map { it.simultaneousEntryGroup }.distinct().size shouldBe 1
                own.single().objectReferences.source shouldBe result.state.objectRef(source)
            }
        }
    }

})
