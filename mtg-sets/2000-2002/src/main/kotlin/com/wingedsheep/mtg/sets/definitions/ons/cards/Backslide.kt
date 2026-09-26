package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Backslide
 * {1}{U}
 * Instant
 * Turn target creature with a morph ability face down.
 * Cycling {U}
 */
val Backslide = card("Backslide") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Turn target creature with a morph ability face down.\nCycling {U}"

    spell {
        val t = target(TargetFilter(GameObjectFilter.Creature.withMorph().faceUp()))
        effect = Effects.TurnFaceDown(t)
    }

    keywordAbility(KeywordAbility.cycling("{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Pete Venters"
        flavorText = "\"Some things are better left unknown.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47c40269-80a5-454f-83dd-dae1c11500c0.jpg?1562911791"
    }
}
