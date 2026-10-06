package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Upheaval reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Odyssey (`ody`) `cards/` package; this file contributes only
 * per-printing presentation data.
 */
val UpheavalReprint = Printing(
    oracleId = "7cafc972-a6f5-4cac-a3d3-8a3ae36ffb1e",
    name = "Upheaval",
    setCode = "MH2",
    collectorNumber = "270",
    scryfallId = "befe74b1-c487-42bb-a1a1-4d13f3a86ff7",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/b/e/befe74b1-c487-42bb-a1a1-4d13f3a86ff7.jpg?1783926787",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    borderColor = "black",
)
