package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Enrage
 * {X}{R}
 * Instant
 * Target creature gets +X/+0 until end of turn.
 */
val Enrage = card("Enrage") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +X/+0 until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(DynamicAmounts.xValue(), DynamicAmounts.fixed(0), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Wayne England"
        flavorText = "A barbarian's heart knows a fire no amount of blood can quench."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6ed7866-9eef-49c3-9b9e-4247b6e71a6c.jpg?1562535239"
    }
}
