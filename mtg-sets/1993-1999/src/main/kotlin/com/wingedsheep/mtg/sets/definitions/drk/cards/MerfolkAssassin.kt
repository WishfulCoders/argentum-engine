package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Merfolk Assassin
 * {U}{U}
 * Creature — Merfolk Assassin
 * 1/2
 * {T}: Destroy target creature with islandwalk.
 */
val MerfolkAssassin = card("Merfolk Assassin") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Assassin"
    power = 1
    toughness = 2
    oracleText = "{T}: Destroy target creature with islandwalk."

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature.withKeyword(Keyword.ISLANDWALK))
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Dennis Detwiller"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36313dc7-6bf2-4d73-b696-969d984a7466.jpg?1783947942"
    }
}
