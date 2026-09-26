package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Battlewise Valor
 * {1}{W}
 * Instant
 *
 * Target creature gets +2/+2 until end of turn. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)
 */
val BattlewiseValor = card("Battlewise Valor") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, target = creature) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Zack Stella"
        flavorText = "It's never good to walk into an ambush, but with the right spell you might walk out again."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d967a4c-2323-4361-a907-5ca9140d8793.jpg"
    }
}
