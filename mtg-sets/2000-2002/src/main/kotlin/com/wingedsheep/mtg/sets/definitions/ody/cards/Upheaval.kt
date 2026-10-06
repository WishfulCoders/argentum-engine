package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Upheaval
 * {4}{U}{U}
 * Sorcery
 * Return all permanents to their owners' hands.
 *
 * A non-targeted mass bounce over every permanent — lands, tokens and the caster's own included —
 * through [Patterns.Group.returnAllToHand], which moves each one to its owner's hand in a single
 * simultaneous move. Tokens go to the hand and then cease to exist (CR 704.5d).
 */
val Upheaval = card("Upheaval") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return all permanents to their owners' hands."

    spell {
        effect = Patterns.Group.returnAllToHand(GroupFilter.AllPermanents)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Kev Walker"
        flavorText = "The calm comes after the storm."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e201229-34a6-48c8-a07c-d8aefcf5f8a7.jpg?1783945252"
    }
}
