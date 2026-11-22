package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EnlistCombatHistorySerializationTest : StringSpec({
    "enlist combat history composes through symbolic entity references" {
        for (entity in listOf(EffectTarget.Self, EffectTarget.TriggeringEntity, EffectTarget.ContextTarget(0))) {
            val original = DynamicAmounts.enlistmentsThisCombat(entity)
            CardSerialization.json.decodeFromString(DynamicAmount.serializer(),
                CardSerialization.json.encodeToString(DynamicAmount.serializer(), original)) shouldBe original
        }
    }
})
