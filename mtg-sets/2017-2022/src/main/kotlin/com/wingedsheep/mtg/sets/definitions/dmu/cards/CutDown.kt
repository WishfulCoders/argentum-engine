package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cut Down
 * {B}
 * Instant
 * Destroy target creature with total power and toughness 5 or less.
 *
 * The filter reads projected power and toughness, so pumps and shrinks applied before
 * casting (and before resolution, when the target is rechecked) count.
 */
val CutDown = card("Cut Down") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature with total power and toughness 5 or less."

    spell {
        val t = target(TargetFilter.Creature.totalPowerAndToughnessAtMost(5))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Dominik Mayer"
        flavorText = "\"There can be no mercy, no half measures. When facing Phyrexians, it's kill swiftly or die.\"\n—Jodah"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/753db072-5d6a-4f37-8f7d-255572ecd3bd.jpg?1783921335"
        ruling(
            "2022-09-09",
            "The total power and toughness of a creature is determined by adding its power and toughness. " +
                "For example, the total power and toughness of a 3/2 creature is 5."
        )
    }
}
