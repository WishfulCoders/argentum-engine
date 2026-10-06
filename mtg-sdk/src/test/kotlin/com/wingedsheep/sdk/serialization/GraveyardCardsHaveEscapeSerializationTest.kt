package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveEscape
import com.wingedsheep.sdk.scripting.StaticAbility
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GraveyardCardsHaveEscapeSerializationTest : StringSpec({
    fun roundTrip(original: StaticAbility): StaticAbility = CardSerialization.json.decodeFromString(
        StaticAbility.serializer(),
        CardSerialization.json.encodeToString(StaticAbility.serializer(), original)
    )

    "the Underworld Breach grant keeps its filter and exile half through polymorphic serialization" {
        val original = GraveyardCardsHaveEscape(
            filter = GameObjectFilter.Nonland,
            additionalCost = Costs.additional.ExileOtherCards(3)
        )
        val restored = roundTrip(original)
        restored shouldBe original
        (restored as GraveyardCardsHaveEscape).cost shouldBe null
        restored.description shouldBe "Each nonland card in your graveyard has escape. The escape cost is " +
            "equal to the card's mana cost plus ${original.additionalCost!!.description.replaceFirstChar { it.lowercase() }}"
    }

    "a fixed-cost grant round-trips its mana cost" {
        val original = GraveyardCardsHaveEscape(filter = GameObjectFilter.Creature, cost = ManaCost.parse("{2}{B}"))
        val restored = roundTrip(original) as GraveyardCardsHaveEscape
        restored shouldBe original
        restored.cost shouldBe ManaCost.parse("{2}{B}")
        restored.additionalCost shouldBe null
    }
})
