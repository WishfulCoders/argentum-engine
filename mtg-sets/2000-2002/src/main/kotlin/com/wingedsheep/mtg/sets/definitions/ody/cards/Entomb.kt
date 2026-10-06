package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Entomb
 * {B}
 * Instant
 * Search your library for a card, put that card into your graveyard, then shuffle.
 *
 * Demonic Tutor's search with a graveyard destination (Buried Alive's shape for one card of any kind).
 */
val Entomb = card("Entomb") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Search your library for a card, put that card into your graveyard, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any,
            destination = SearchDestination.GRAVEYARD,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Ron Spears"
        flavorText = "A grave is the safest place to store ill-gotten treasures."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f60a2091-fb97-4f04-911b-fce9b6351044.jpg?1783945246"
    }
}
