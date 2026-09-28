package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mana Leak reprint in Magic 2011. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in STH's `cards/` package (the card's earliest real printing); this file contributes
 * only the M11 presentation row.
 */
val ManaLeakReprint = Printing(
    oracleId = "c61fe162-2202-4e56-9ba0-393547f9875f",
    name = "Mana Leak",
    setCode = "M11",
    collectorNumber = "62",
    scryfallId = "a7c7757d-8036-4b33-a1cb-07795d392588",
    artist = "Howard Lyon",
    imageUri = "https://cards.scryfall.io/normal/front/a/7/a7c7757d-8036-4b33-a1cb-07795d392588.jpg?1783941824",
    releaseDate = "2010-07-16",
    rarity = Rarity.COMMON,
)
