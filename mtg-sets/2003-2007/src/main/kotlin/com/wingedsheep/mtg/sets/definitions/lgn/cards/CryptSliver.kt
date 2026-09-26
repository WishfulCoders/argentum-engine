package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crypt Sliver
 * {1}{B}
 * Creature — Sliver
 * 1/1
 * All Slivers have "{T}: Regenerate target Sliver."
 */
val CryptSliver = card("Crypt Sliver") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Sliver"
    power = 1
    toughness = 1
    oracleText = "All Slivers have \"{T}: Regenerate target Sliver.\""

    val sliverFilter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Sliver"))

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val creature = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Sliver")))
                effect = Effects.Regenerate(creature)
            },
            filter = sliverFilter
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"Death couldn't contain the slivers. What made we think we could?\" —Riptide Project researcher"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/507097eb-6b50-47ae-a545-df76b743b2bd.jpg?1562911300"
    }
}
