package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cut Down — Dominaria United #89 (canonical printing)
 * {B} · Instant
 *
 * Destroy target creature with total power and toughness 5 or less.
 *
 * The sum is read from projected power and toughness, so pumps and shrinks count, both when
 * targeting and when the target is rechecked on resolution.
 */
val CutDown = card("Cut Down") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature with total power and toughness 5 or less."

    spell {
        val creature = target(TargetFilter.Creature.totalPowerAndToughnessAtMost(5))
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Dominik Mayer"
        flavorText = "\"There can be no mercy, no half measures. When facing Phyrexians, it's kill " +
            "swiftly or die.\"\n—Jodah"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/753db072-5d6a-4f37-8f7d-255572ecd3bd.jpg?1783921335"
        ruling(
            "2022-09-09",
            "The total power and toughness of a creature is determined by adding its power and " +
                "toughness. For example, the total power and toughness of a 3/2 creature is 5."
        )
    }
}
