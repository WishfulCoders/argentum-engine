package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

private const val offerText =
    "Tempting offer — Search your library for a land card and put it onto the battlefield. " +
        "Each opponent may search their library for a land card and put it onto the battlefield. " +
        "For each opponent who searches a library this way, search your library for a land card and put it onto the battlefield. " +
        "Then each player who searched a library this way shuffles."

/**
 * Tempt with Discovery
 * {3}{G}
 * Sorcery
 * Tempting offer — Search your library for a land card and put it onto the battlefield. Each
 * opponent may search their library for a land card and put it onto the battlefield. For each
 * opponent who searches a library this way, search your library for a land card and put it onto
 * the battlefield. Then each player who searched a library this way shuffles.
 *
 * [Patterns.Mechanic.temptingOffer] runs the search for you, collects every opponent's yes/no in
 * turn order, then searches for each accepting opponent and once more for you per acceptance.
 * Each search shuffles its own library as it finishes rather than every searcher shuffling at the
 * very end; a library is only ever searched by its owner here, so nothing can observe the order
 * between one player's shuffle and another player's search.
 */
val TemptWithDiscovery = card("Tempt with Discovery") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = offerText

    spell {
        effect = Patterns.Mechanic.temptingOffer(
            offer = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Land,
                destination = SearchDestination.BATTLEFIELD,
            ),
            description = offerText,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "William Wu"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00bfc923-4f6b-4c6f-b74f-ddf95a3459f8.jpg?1783939655"
        ruling("2013-10-17", "Your opponents decide in turn order whether or not they accept the offer, starting with the opponent on your left. Each opponent will know the decisions of previous opponents in turn order when making their decision.")
        ruling("2013-10-17", "After each opponent has decided, the effect happens simultaneously for each one who accepted the offer. Then, the effect happens again for you a number of times equal to the number of opponents who accepted.")
    }
}
