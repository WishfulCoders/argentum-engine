package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bring Low
 * {3}{R}
 * Instant
 * Bring Low deals 3 damage to target creature. If that creature has a +1/+1 counter on it,
 * Bring Low deals 5 damage to it instead.
 */
val BringLow = card("Bring Low") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Bring Low deals 3 damage to target creature. If that creature has a +1/+1 counter on it, Bring Low deals 5 damage to it instead."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.conditional(
                condition = Conditions.TargetHasCounter(CounterType.PLUS_ONE_PLUS_ONE, creature),
                ifTrue = 5,
                ifFalse = 3
            ),
            target = creature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Slawomir Maniak"
        flavorText = "\"People are often humbled by the elements. But the elements, too, can be humbled.\"\n—Surrak, khan of the Temur"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9ba5e7bf-2ad8-4061-937a-ef1e9b63da3d.jpg?1562790998"
    }
}
