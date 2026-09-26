package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Depressurize
 * {1}{B}
 * Instant
 * Target creature gets -3/-0 until end of turn. Then if that creature's power is 0 or less, destroy it.
 */
val Depressurize = card("Depressurize") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -3/-0 until end of turn. Then if that creature's power is 0 or less, destroy it."

    // Main spell effect
    spell {
        val target = target(TargetFilter.Creature)
        
        // Apply -3/-0 debuff
        effect = Effects.ModifyStats(-3, 0, target) then
            // Then destroy if power is 0 or less
            Effects.If(
                condition = Conditions.TargetPowerAtMost(DynamicAmounts.fixed(0), target),
                then = Effects.Destroy(target)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Danny Schwartz"
        flavorText = "The Sunstar faith teaches of Perfect Void—of emptying oneself so that the light may fill the hollow. But in the void of death, its teachings bring little comfort."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25520d5a-1a83-42cc-8ace-8b1156019d64.jpg?1752946939"
    }
}
