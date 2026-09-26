package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Magma Sliver
 * {3}{R}
 * Creature — Sliver
 * 3/3
 * All Slivers have "{T}: Target Sliver creature gets +X/+0 until end of turn,
 * where X is the number of Slivers on the battlefield."
 */
val MagmaSliver = card("Magma Sliver") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Sliver"
    power = 3
    toughness = 3
    oracleText = "All Slivers have \"{T}: Target Sliver creature gets +X/+0 until end of turn, where X is the number of Slivers on the battlefield.\""

    val sliverFilter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Sliver"))

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val creature = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Sliver")))
                effect = Effects.ModifyStats(
                    power = DynamicAmounts.permanentsWithSubtype(Subtype.SLIVER),
                    toughness = DynamicAmounts.fixed(0),
                    target = creature
                )
            },
            filter = sliverFilter
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Wayne England"
        flavorText = "As malleable as molten steel, but as dangerous as the finished blade."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9091d908-456f-4127-857d-b22fdb4f2fd9.jpg?1562924149"
    }
}
