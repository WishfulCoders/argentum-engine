package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class RandomizedBlockerPilesSerializationTest : FunSpec({
    test("duration survives polymorphic effect serialization") {
        for (duration in listOf(Duration.EndOfTurn, Duration.EndOfCombat, Duration.Permanent)) {
            val effect = Effects.RandomizedBlockerPiles(duration)
            val encoded = CardSerialization.json.encodeToString<Effect>(effect)
            CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
        }
    }
})
