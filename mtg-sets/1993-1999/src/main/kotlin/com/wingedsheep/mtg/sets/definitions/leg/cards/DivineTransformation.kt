package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Divine Transformation
 * {2}{W}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +3/+3.
 */
val DivineTransformation = card("Divine Transformation") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +3/+3."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "NéNé Thomas"
        flavorText = "Glory surged through her and radiance surrounded her. All things were possible with the " +
            "blessing of the Divine."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a89ad9fd-33a6-4d31-9f4c-8bf192882f21.jpg?1783948086"
    }
}
