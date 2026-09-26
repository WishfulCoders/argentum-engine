package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mythic Proportions
 * {4}{G}{G}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +8/+8 and has trample.
 */
val MythicProportions = card("Mythic Proportions") {
    manaCost = "{4}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +8/+8 and has trample."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(8, 8)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "274"
        artist = "Jim Nelson"
        flavorText = "The blood of Krosa turns rational beings into primal forces."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/829069cf-7e63-4443-b679-65ad15d6ca5e.jpg?1562925823"
    }
}
