package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Crop Rotation — Urza's Legacy #98 (canonical printing)
 * {G} · Instant
 *
 * As an additional cost to cast this spell, sacrifice a land.
 * Search your library for a land card, put that card onto the battlefield, then shuffle.
 *
 * The Harrow shape with any land card (not just basics) and a single find.
 */
val CropRotation = card("Crop Rotation") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a land.\n" +
        "Search your library for a land card, put that card onto the battlefield, then shuffle."

    additionalCost(Costs.additional.SacrificePermanent(filter = GameObjectFilter.Land))

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land,
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "DiTerlizzi"
        flavorText = "\"Hmm . . . maybe lotuses this year.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6563f790-862c-465a-b963-7a61f2385516.jpg?1783946231"
        ruling("2022-12-08", "You can’t cast Crop Rotation without sacrificing a land, and you can’t sacrifice additional lands.")
    }
}
