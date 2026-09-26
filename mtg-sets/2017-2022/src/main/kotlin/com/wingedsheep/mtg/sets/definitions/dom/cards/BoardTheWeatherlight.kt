package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.dsl.Effects

/**
 * Board the Weatherlight
 * {1}{W}
 * Sorcery
 * Look at the top five cards of your library. You may reveal a historic card from among
 * them and put it into your hand. Put the rest on the bottom of your library in a random order.
 *
 * (A card is historic if it has the legendary supertype, the artifact card type, or the Saga subtype.)
 */
val BoardTheWeatherlight = card("Board the Weatherlight") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. You may reveal a historic card from among them and put it into your hand. Put the rest on the bottom of your library in a random order. (A card is historic if it has the legendary supertype, the artifact card type, or the Saga subtype.)"

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(5))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Historic,
                prompt = "You may reveal a historic card and put it into your hand",
                showAllCards = true
            )
            toHand(kept)
            toLibraryBottom(rest, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Tyler Jacobson"
        flavorText = "A new gathering for a new age."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a11d10fa-d42d-4867-9e2f-fc8de582d5e7.jpg?1562740473"
        ruling("2018-04-27", "A card, spell, or permanent is historic if it has the legendary supertype, the artifact card type, or the Saga subtype.")
    }
}
