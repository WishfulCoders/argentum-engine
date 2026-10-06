package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Memory Lapse reprint in Mirage. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in HML's `cards/` package (the card's earliest real printing); this file contributes
 * only the MIR presentation row.
 */
val MemoryLapseReprint = Printing(
    oracleId = "bbfb3e4a-b389-4391-8141-13b68c0ef2e0",
    name = "Memory Lapse",
    setCode = "MIR",
    collectorNumber = "74",
    scryfallId = "63453ed9-5cf1-4cad-b173-a067f22a4405",
    artist = "Rebecca Guay",
    imageUri = "https://cards.scryfall.io/normal/front/6/3/63453ed9-5cf1-4cad-b173-a067f22a4405.jpg?1783947111",
    releaseDate = "1996-10-08",
    rarity = Rarity.COMMON,
)
