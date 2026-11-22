package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TriggerMatcherSourceFilterTest : FunSpec({
    val source = EntityId.of("source")
    val other = EntityId.of("other")
    val controller = EntityId.of("controller")
    val predicates = PredicateEvaluator(cardRegistry = null)
    val matcher = TriggerMatcher(predicates, predicates.conditions)

    for ((from, to) in listOf(Zone.HAND to Zone.BATTLEFIELD, Zone.BATTLEFIELD to Zone.GRAVEYARD)) {
        fun matches(id: EntityId, filter: GameObjectFilter) = matcher.matchesZoneChangeTrigger(
            EventPattern.ZoneChangeEvent(filter = filter, from = from, to = to),
            TriggerBinding.ANY,
            ZoneChangeEvent(id, "Object", from, to, controller),
            source, controller, GameState(),
        )
        test("source-only zone-change filter compares observer identity for $from to $to") {
            matches(source, GameObjectFilter.Any.sourceItself()) shouldBe true
            matches(other, GameObjectFilter.Any.sourceItself()) shouldBe false
        }
        test("negated source zone-change filter excludes only observer for $from to $to") {
            matches(source, GameObjectFilter.Any.notSourceItself()) shouldBe false
            matches(other, GameObjectFilter.Any.notSourceItself()) shouldBe true
        }
        test("source union preserves each branch identity for $from to $to") {
            val filter = GameObjectFilter.Any.sourceItself() or GameObjectFilter.Creature
            matches(source, filter) shouldBe true
            matches(other, filter) shouldBe false
        }
    }
})
