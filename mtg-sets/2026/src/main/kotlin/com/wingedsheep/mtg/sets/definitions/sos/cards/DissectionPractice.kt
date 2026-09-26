package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Targets

/**
 * Dissection Practice
 * {B}
 * Instant
 * Target opponent loses 1 life and you gain 1 life.
 * Up to one target creature gets +1/+1 until end of turn.
 * Up to one target creature gets -1/-1 until end of turn.
 */
val DissectionPractice = card("Dissection Practice") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target opponent loses 1 life and you gain 1 life.\n" +
        "Up to one target creature gets +1/+1 until end of turn.\n" +
        "Up to one target creature gets -1/-1 until end of turn."
    spell {
        val opponent = target(Targets.Opponent)
        val pumped = target(TargetFilter.Creature, optional = true)
        val weakened = target(TargetFilter.Creature, optional = true)
        effect = Effects.LoseLife(1, opponent) then
            Effects.GainLife(1) then
            Effects.ModifyStats(1, 1, pumped) then
            Effects.ModifyStats(-1, -1, weakened)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Steve Ellis"
        flavorText = "From the very first incision, Enid regretted choosing Witherbloom for her intercollege elective."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddbf1242-6832-475e-9a77-65dd9b4bb32a.jpg"
    }
}
