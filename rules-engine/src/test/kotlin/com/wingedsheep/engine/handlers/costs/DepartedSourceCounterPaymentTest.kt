package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DepartedSourceCounterPaymentTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for (faceDown in listOf(false, true)) {
        test("missing token source can pay counters using ${if (faceDown) "masked" else "last-known"} source metadata") {
            val d = driver()
            val source = EntityId.generate()
            val land = d.putLandOnBattlefield(d.player1, "Forest")
            d.addComponent(land, CountersComponent(mapOf(CounterType.QUEST to 3)))
            val context = EffectContext(sourceId = source, controllerId = d.player1,
                lastKnownSourceSnapshot = EntitySnapshot(source, name = "Departed Test Token",
                    wasToken = true, wasFaceDown = faceDown))
            val result = d.services.effectExecutorRegistry.execute(d.state,
                Effects.PayOrSuffer(Costs.pay.RemoveCounters(count = 3, counterType = CounterType.QUEST),
                    Effects.Nothing), context)
            result.error shouldBe null
            val decision = result.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.context.sourceName shouldBe if (faceDown) "Face-down source" else "Departed Test Token"
            decision.options shouldBe listOf(land)
            decision.selectionLimits shouldBe mapOf(land to 3)
            d.replaceState(result.state)
            d.submitCardSelection(d.player1, List(3) { land }).error shouldBe null
            d.state.getEntity(land)!!.get<CountersComponent>()!!.getCount(CounterType.QUEST) shouldBe 0
            d.state.getEntity(source) shouldBe null
        }
    }

    for (faceDown in listOf(false, true)) {
        test("actual token departure captures source name and face-down metadata ($faceDown)") {
            val d = driver()
            val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            d.addComponent(source, com.wingedsheep.engine.state.components.identity.TokenComponent)
            if (faceDown) d.addComponent(source, com.wingedsheep.engine.state.components.identity.FaceDownComponent)
            val moved = d.services.zones.moveToZone(d.state, source, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
            val snapshot = moved.events.filterIsInstance<com.wingedsheep.engine.core.ZoneChangeEvent>()
                .single { it.entityId == source }.lastKnown.shouldNotBeNull()
            snapshot.name shouldBe "Grizzly Bears"
            snapshot.wasFaceDown shouldBe faceDown
            snapshot.wasToken shouldBe true
            // Tokens cease to exist before a later payment resolves; use the actual departure
            // snapshot, rather than a synthetic snapshot with a manually supplied name.
            d.replaceState(moved.state.removeEntity(source))
            val land = d.putLandOnBattlefield(d.player1, "Forest")
            d.addComponent(land, CountersComponent(mapOf(CounterType.QUEST to 3)))
            val result = d.services.effectExecutorRegistry.execute(d.state,
                Effects.PayOrSuffer(Costs.pay.RemoveCounters(count = 3, counterType = CounterType.QUEST),
                    Effects.Nothing), EffectContext(sourceId = source, controllerId = d.player1,
                    lastKnownSourceSnapshot = snapshot))
            result.error shouldBe null
            result.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().context.sourceName shouldBe
                if (faceDown) "Face-down source" else "Grizzly Bears"
        }
    }

    test("missing OwnManaCost source fails closed instead of becoming a free payment") {
        val d = driver()
        val source = EntityId.generate()
        val context = EffectContext(sourceId = source, controllerId = d.player1,
            lastKnownSourceSnapshot = EntitySnapshot(source, name = "Departed Test Token", wasToken = true))
        val before = d.state
        val result = d.services.effectExecutorRegistry.execute(before,
            Effects.PayOrSuffer(Costs.pay.OwnManaCost, Effects.Nothing), context)
        result.error.shouldNotBeNull()
        result.pendingDecision shouldBe null
        result.state shouldBe before
    }
})
