package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.serialization.CardSerialization
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit coverage for the Station DSL surface (CR 702.184):
 *  - the `station()` card-builder helper emits the fixed station keyword ability,
 *  - it uses [DynamicAmount.StationCharge] (so the 702.184c toughness substitution stays scoped),
 *  - [Conditions.SourceCounterCountAtLeast] builds the `{N+}` threshold gate,
 *  - the whole thing round-trips through JSON.
 */
class StationDslTest : DescribeSpec({

    describe("station() builder") {

        val rammer = card("Test Spacecraft") {
            typeLine = "Artifact — Spacecraft"
            power = 3
            toughness = 4
            station()
        }

        it("emits exactly one activated ability with the CR 702.184a cost and effect") {
            rammer.activatedAbilities.size shouldBe 1
            val station = rammer.activatedAbilities.single()

            val cost = station.cost.shouldBeInstanceOf<AbilityCost.Atom>()
            val atom = cost.atom.shouldBeInstanceOf<CostAtom.TapPermanents>()
            atom.count shouldBe 1
            atom.excludeSelf shouldBe true
            atom.filter shouldBe GameObjectFilter.Creature

            station.timing shouldBe TimingRule.SorcerySpeed

            val effect = station.effect.shouldBeInstanceOf<AddDynamicCountersEffect>()
            effect.counterType shouldBe CounterType.CHARGE
            effect.amount shouldBe DynamicAmount.StationCharge
        }

        it("round-trips through JSON") {
            val json = CardSerialization.json
            val encoded = json.encodeToString(com.wingedsheep.sdk.model.CardDefinition.serializer(), rammer)
            val decoded = json.decodeFromString(com.wingedsheep.sdk.model.CardDefinition.serializer(), encoded)
            decoded shouldBe rammer
            // StationCharge serializes as a bare object discriminator, not an EntityProperty read.
            encoded.shouldContainStationCharge()
        }
    }

    describe("Conditions.SourceCounterCountAtLeast") {

        it("is the {N+} charge-counter threshold gate (CR 721.2a)") {
            val cond = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 9)
            val compare = cond.shouldBeInstanceOf<Compare>()
            compare.operator shouldBe ComparisonOperator.GTE
            compare.right shouldBe DynamicAmount.Fixed(9)
        }
    }
})

private fun String.shouldContainStationCharge() {
    check("StationCharge" in this) { "expected serialized form to contain StationCharge, was: $this" }
}
