package com.wingedsheep.mtg.sets.definitions.arc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Reanimate reprint in Archenemy. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in TMP's `cards/` package (the card's earliest real printing); this file contributes
 * only the ARC presentation row.
 */
val ReanimateReprint = Printing(
    oracleId = "a044474a-cd72-4e9d-bd8d-a08f2de9cdc0",
    name = "Reanimate",
    setCode = "ARC",
    collectorNumber = "21",
    scryfallId = "bee3dad4-88d8-424c-b3f8-d089b6891fb8",
    artist = "Robert Bliss",
    imageUri = "https://cards.scryfall.io/normal/front/b/e/bee3dad4-88d8-424c-b3f8-d089b6891fb8.jpg?1783941913",
    releaseDate = "2010-06-18",
    rarity = Rarity.UNCOMMON,
)
