package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mana Leak reprint in Eighth Edition. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in STH's `cards/` package (the card's earliest real printing); this file contributes
 * only the 8ED presentation row.
 */
val ManaLeakReprint = Printing(
    oracleId = "c61fe162-2202-4e56-9ba0-393547f9875f",
    name = "Mana Leak",
    setCode = "8ED",
    collectorNumber = "89",
    scryfallId = "56707af4-eb74-4b33-8741-6cc9b547d919",
    artist = "Christopher Rush",
    imageUri = "https://cards.scryfall.io/normal/front/5/6/56707af4-eb74-4b33-8741-6cc9b547d919.jpg?1783944794",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
