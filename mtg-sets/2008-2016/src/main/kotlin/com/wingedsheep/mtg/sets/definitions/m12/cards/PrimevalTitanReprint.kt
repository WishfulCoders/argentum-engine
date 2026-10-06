package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Primeval Titan reprint in Magic 2012. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Magic 2011 (`m11`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val PrimevalTitanReprint = Printing(
    oracleId = "ae83ef2c-960f-4c5b-97cc-52465c687c18",
    name = "Primeval Titan",
    setCode = "M12",
    collectorNumber = "188",
    scryfallId = "fd6ddbca-b943-49d6-b341-509bb72dd5a6",
    artist = "Aleksi Briclot",
    imageUri = "https://cards.scryfall.io/normal/front/f/d/fd6ddbca-b943-49d6-b341-509bb72dd5a6.jpg?1783941057",
    releaseDate = "2011-07-15",
    rarity = Rarity.MYTHIC,
)
