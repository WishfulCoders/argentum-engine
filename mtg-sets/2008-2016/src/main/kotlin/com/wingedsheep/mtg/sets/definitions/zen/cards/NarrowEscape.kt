package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Narrow Escape
 * {2}{W}
 * Instant
 * Return target permanent you control to its owner's hand. You gain 4 life.
 */
val NarrowEscape = card("Narrow Escape") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Return target permanent you control to its owner's hand. You gain 4 life."

    spell {
        val permanent = target(TargetFilter.PermanentYouControl)
        effect = Effects.ReturnToHand(permanent) then Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Karl Kopinski"
        flavorText = "\"A good explorer has to be as slippery as a gomazoa, as tough as a scute bug, and luckier than a ten-fingered trapfinder.\"\n—Arhana, Kazandu trapfinder"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c9942a2-11d2-403b-89c6-35c23ac9e9da.jpg"
    }
}
