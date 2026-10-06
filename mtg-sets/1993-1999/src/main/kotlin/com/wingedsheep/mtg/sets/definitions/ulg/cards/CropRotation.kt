package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Crop Rotation
 * {G}
 * Instant
 * As an additional cost to cast this spell, sacrifice a land.
 * Search your library for a land card, put that card onto the battlefield, then shuffle.
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
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "DiTerlizzi"
        flavorText = "\"Hmm . . . maybe lotuses this year.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6563f790-862c-465a-b963-7a61f2385516.jpg?1783946231"

        ruling("2022-12-08", "You can't cast Crop Rotation without sacrificing a land, and you can't sacrifice additional lands.")
    }
}
