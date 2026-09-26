package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Targets

/**
 * Crater's Claws
 * {X}{R}
 * Sorcery
 * Crater's Claws deals X damage to any target.
 * Ferocious — Crater's Claws deals X plus 2 damage instead if you control a creature with power 4 or greater.
 */
val CratersClaws = card("Crater's Claws") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Crater's Claws deals X damage to any target.\nFerocious — Crater's Claws deals X plus 2 damage instead if you control a creature with power 4 or greater."

    spell {
        val t = target(Targets.Any)
        effect = Effects.If(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
            then = Effects.DealDamage(DynamicAmounts.xValue() + 2, t),
            otherwise = Effects.DealDamage(DynamicAmounts.xValue(), t)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95dde66b-b4a1-4a1e-8c9e-0bec4790b1e5.jpg?1562790652"
    }
}
