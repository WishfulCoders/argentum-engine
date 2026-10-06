package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Primeval Titan — Magic 2011 #192
 * {4}{G}{G} · Creature — Giant · 6 / 6
 *
 * Trample
 * Whenever this creature enters or attacks, you may search your library for up to two land cards,
 * put them onto the battlefield tapped, then shuffle.
 *
 * "Enters or attacks" is two triggered abilities sharing one optional effect — Primeval Herald's
 * shape with any land card (ruling) and up to two of them.
 */
val PrimevalTitan = card("Primeval Titan") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant"
    power = 6
    toughness = 6
    oracleText = "Trample\n" +
        "Whenever this creature enters or attacks, you may search your library for up to two land cards, " +
        "put them onto the battlefield tapped, then shuffle."

    keywords(Keyword.TRAMPLE)

    val fetchTwoLands = Effects.May(
        Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = fetchTwoLands
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = fetchTwoLands
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "192"
        artist = "Aleksi Briclot"
        flavorText = "When nature calls, *run*."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feee9327-b937-46ba-a2aa-6c015ab6cdd5.jpg?1783941794"

        ruling("2017-11-17", "You may find any land cards, not just basic land cards.")
    }
}
