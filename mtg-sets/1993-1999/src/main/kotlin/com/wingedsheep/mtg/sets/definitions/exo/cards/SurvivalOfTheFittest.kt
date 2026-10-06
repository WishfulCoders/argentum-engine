package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Survival of the Fittest
 * {1}{G}
 * Enchantment
 * {G}, Discard a creature card: Search your library for a creature card, reveal that card, put it
 * into your hand, then shuffle.
 *
 * The search is for a card with a stated quality, so it may fail to find (the search pipeline lets
 * the player pick nothing).
 */
val SurvivalOfTheFittest = card("Survival of the Fittest") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "{G}, Discard a creature card: Search your library for a creature card, reveal that card, " +
        "put it into your hand, then shuffle."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G}"),
            Costs.Discard(GameObjectFilter.Creature)
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature,
            destination = SearchDestination.HAND,
            reveal = true
        )
        description = "{G}, Discard a creature card: Search your library for a creature card, reveal it, " +
            "put it into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c060c178-3c0e-493f-b6f0-ead5b1d6f191.jpg?1783946500"

        ruling("2004-10-04", "Because the \"search\" requires you to find a card with certain characteristics, you don't have to find the card if you don't want to.")
    }
}
