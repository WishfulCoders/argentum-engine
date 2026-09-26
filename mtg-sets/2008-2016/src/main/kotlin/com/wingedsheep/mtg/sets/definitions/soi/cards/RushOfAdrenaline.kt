package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rush of Adrenaline
 * {R}
 * Instant
 * Target creature gets +2/+1 and gains trample until end of turn.
 */
val RushOfAdrenaline = card("Rush of Adrenaline") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+1 and gains trample until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(power = 2, toughness = 1, target = t) then
            Effects.GrantKeyword(Keyword.TRAMPLE, target = t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Chris Rallis"
        flavorText = "Scarecrows only go so far in sending the message to stay away."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0def54b-9f0a-4ab1-9df9-25506a06350c.jpg?1783937744"
    }
}
