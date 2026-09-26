package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rumbling Rockslide — {3}{R}
 * Sorcery
 * Rumbling Rockslide deals damage to target creature equal to the number of lands you control.
 */
val RumblingRockslide = card("Rumbling Rockslide") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Rumbling Rockslide deals damage to target creature equal to the number of lands you control."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.landsYouControl(),
            target = t,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Adam Paquette"
        flavorText = "\"When monsters walk, the earth knows its place and yields.\"\n—Rielle, the Everwise"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96f9aaa7-11c7-4cd0-9803-9471c14ab846.jpg"
    }
}
