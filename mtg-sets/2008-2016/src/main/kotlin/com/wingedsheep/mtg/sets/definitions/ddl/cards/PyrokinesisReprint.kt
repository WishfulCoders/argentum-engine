package com.wingedsheep.mtg.sets.definitions.ddl.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pyrokinesis reprint in Duel Decks: Heroes vs. Monsters. Canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in another set's `cards/` package; this file contributes only presentation data.
 */
val PyrokinesisReprint = Printing(
    oracleId = "d001febc-d511-4e65-a631-91dc9415056b",
    name = "Pyrokinesis",
    setCode = "DDL",
    collectorNumber = "32",
    scryfallId = "62c403ad-e550-4f5d-afd8-d8393731fa9c",
    artist = "Igor Kieryluk",
    imageUri = "https://cards.scryfall.io/normal/front/6/2/62c403ad-e550-4f5d-afd8-d8393731fa9c.jpg?1783939851",
    releaseDate = "2013-09-06",
    rarity = Rarity.UNCOMMON,
)
