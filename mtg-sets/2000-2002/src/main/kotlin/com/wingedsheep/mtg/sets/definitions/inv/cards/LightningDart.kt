package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lightning Dart
 * {1}{R}
 * Instant
 * Lightning Dart deals 1 damage to target creature. If that creature is white
 * or blue, Lightning Dart deals 4 damage to it instead.
 */
val LightningDart = card("Lightning Dart") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Lightning Dart deals 1 damage to target creature. If that creature is white or blue, " +
        "Lightning Dart deals 4 damage to it instead."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(Filters.Creature.withAnyColor(Color.WHITE, Color.BLUE), t),
            then = Effects.DealDamage(4, t),
            otherwise = Effects.DealDamage(1, t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54d05157-d154-4203-bf3e-add110cb1cee.jpg?1562912254"
    }
}
