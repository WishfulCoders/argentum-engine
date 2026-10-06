package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Destroy Evil
 * {1}{W}
 * Instant
 * Choose one —
 * • Destroy target creature with toughness 4 or greater.
 * • Destroy target enchantment.
 */
val DestroyEvil = card("Destroy Evil") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Destroy target creature with toughness 4 or greater.\n• Destroy target enchantment."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target creature with toughness 4 or greater") {
                val t = target(TargetFilter.Creature.toughnessAtLeast(4))
                effect = Effects.Destroy(t)
            }
            mode("Destroy target enchantment") {
                val t = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Anna Christenson"
        flavorText = "Serra's grace most often manifests as a healing touch, but it may also grant a merciful death."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f7862ef-2c8d-4d28-9e50-7cc41861f245.jpg?1783921366"
    }
}
