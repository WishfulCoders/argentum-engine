package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Tinker
 * {2}{U}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice an artifact.
 * Search your library for an artifact card, put that card onto the battlefield, then shuffle.
 *
 * The Natural Order shape: a sacrifice-a-permanent additional cost paid as the spell is cast, and a
 * library search straight onto the battlefield. The search may fail to find (a hidden-zone search
 * for a card with a stated quality, CR 701.19b).
 */
val Tinker = card("Tinker") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact.\n" +
        "Search your library for an artifact card, put that card onto the battlefield, then shuffle."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact))

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact,
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Mike Raabe"
        flavorText = "\"I wonder how it feels to be bored.\"\n—Jhoira, artificer"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7da23b15-dfb8-4267-9b33-d7a4c035c434.jpg?1783946244"
        ruling(
            "2004-10-04",
            "Because the \"search\" requires you to find a card with certain characteristics, you don't " +
                "have to find the card if you don't want to."
        )
    }
}
