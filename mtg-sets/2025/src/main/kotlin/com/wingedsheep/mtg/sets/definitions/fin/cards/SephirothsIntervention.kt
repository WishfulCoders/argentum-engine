package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter


/**
 * Sephiroth's Intervention
 * {3}{B}
 * Instant
 * Destroy target creature. You gain 2 life.
 */
val SephirothsIntervention = card("Sephiroth's Intervention") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature. You gain 2 life."
    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true) then Effects.GainLife(2)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Joshua Raphael"
        flavorText = "\"Fate is not to be taken lightly, Cloud.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf7df82f-937a-443f-813f-2bcc6944c5c0.jpg"
    }
}
