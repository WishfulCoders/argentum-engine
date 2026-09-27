package com.wingedsheep.engine.state

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * [EntityMap] must be indistinguishable from the `LinkedHashMap` that `entities + (id to c)` and
 * `entities - id` produced — iteration order above all, since arena games replay bit-identically.
 */
class EntityMapTest : FunSpec({

    fun container(n: Int): ComponentContainer =
        if (n % 2 == 0) ComponentContainer.EMPTY else ComponentContainer.EMPTY.with(TappedComponent)

    test("random puts and removes iterate, look up and compare exactly like a LinkedHashMap") {
        repeat(200) { seed ->
            val rng = Random(seed)
            val ids = (0 until 30).map { EntityId("e$it") }
            var reference: Map<EntityId, ComponentContainer> = ids.take(10).associateWith { container(0) }
            var map: Map<EntityId, ComponentContainer> = EntityMap.of(reference)
            repeat(300) { step ->
                val id = ids[rng.nextInt(ids.size)]
                if (rng.nextInt(4) == 0) {
                    reference = reference - id
                    map = (map as EntityMap).remove(id)
                } else {
                    val value = container(step)
                    reference = reference + (id to value)
                    map = (map as EntityMap).put(id, value)
                }
                map.entries.map { it.key to it.value } shouldBe reference.entries.map { it.key to it.value }
                map.keys.toList() shouldBe reference.keys.toList()
                map.size shouldBe reference.size
                for (probe in ids) {
                    map[probe] shouldBe reference[probe]
                    map.containsKey(probe) shouldBe reference.containsKey(probe)
                }
                (map == reference) shouldBe true
                map.hashCode() shouldBe reference.hashCode()
            }
        }
    }
})
