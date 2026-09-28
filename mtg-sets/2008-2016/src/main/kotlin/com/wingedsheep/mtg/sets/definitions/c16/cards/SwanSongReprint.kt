package com.wingedsheep.mtg.sets.definitions.c16.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Swan Song reprint in Commander 2016. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in THS's `cards/` package (the card's earliest real printing); this file contributes
 * only the C16 presentation row.
 */
val SwanSongReprint = Printing(
    oracleId = "8ddfc283-c9b4-41a5-af88-cf0068e986cc",
    name = "Swan Song",
    setCode = "C16",
    collectorNumber = "98",
    scryfallId = "9d968dde-c406-48ef-a1ab-373aebc24693",
    artist = "Peter Mohrbacher",
    imageUri = "https://cards.scryfall.io/normal/front/9/d/9d968dde-c406-48ef-a1ab-373aebc24693.jpg?1783937071",
    releaseDate = "2016-11-11",
    rarity = Rarity.RARE,
)
