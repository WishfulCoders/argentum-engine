package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NumericTargetEqualitySerializationTest : FunSpec({
    test("numeric-property equality round-trips and defaults to unconstrained") {
        val req: TargetRequirement = TargetObject(filter = TargetFilter.Creature, unlimited = true,
            sameNumericProperty = CardNumericProperty.TOUGHNESS)
        Json.decodeFromString<TargetRequirement>(Json.encodeToString(req)) shouldBe req
        TargetObject(filter = TargetFilter.Creature).sameNumericProperty shouldBe null
        req.description shouldBe "any number of target creatures with equal toughness"
    }
})
