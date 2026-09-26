package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain

class TriggeredAbilityDescriptionTest : DescribeSpec({

    describe("TriggeredAbility.description and the 'Do this only once each turn' rider") {

        it("appends the printed rider when effectOncePerTurn is set and no override is given") {
            val ability = TriggeredAbility.create(
                id = AbilityId("TriggeredAbilityDescriptionTest_1"),
                trigger = EventPattern.LifeGainEvent(),
                effect = Effects.GainLife(2),
                effectOncePerTurn = true,
            )
            ability.description shouldEndWith " Do this only once each turn."
        }

        it("does not append it for the trigger cap (oncePerTurn), which prints different text") {
            val ability = TriggeredAbility.create(
                id = AbilityId("TriggeredAbilityDescriptionTest_2"),
                trigger = EventPattern.LifeGainEvent(),
                effect = Effects.GainLife(2),
                oncePerTurn = true,
            )
            ability.description shouldNotContain "Do this only once each turn"
        }

        it("lets descriptionOverride win, as both shipped cards rely on") {
            val ability = TriggeredAbility.create(
                id = AbilityId("TriggeredAbilityDescriptionTest_3"),
                trigger = EventPattern.LifeGainEvent(),
                effect = Effects.GainLife(2),
                effectOncePerTurn = true,
                descriptionOverride = "Hand-written text.",
            )
            ability.description shouldBe "Hand-written text."
        }
    }
})
