package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mana Leak reprint in Magic 2012. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in STH's `cards/` package (the card's earliest real printing); this file contributes
 * only the M12 presentation row.
 */
val ManaLeakReprint = Printing(
    oracleId = "c61fe162-2202-4e56-9ba0-393547f9875f",
    name = "Mana Leak",
    setCode = "M12",
    collectorNumber = "63",
    scryfallId = "6b123efa-8631-4a07-970d-ff4f980a0522",
    artist = "Howard Lyon",
    imageUri = "https://cards.scryfall.io/normal/front/6/b/6b123efa-8631-4a07-970d-ff4f980a0522.jpg?1783941090",
    releaseDate = "2011-07-15",
    rarity = Rarity.COMMON,
)
