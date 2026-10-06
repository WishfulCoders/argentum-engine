package com.wingedsheep.mtg.sets.definitions.pz2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fractured Identity reprint in Treasure Chest (PZ2).
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in C17's `cards/` package (the
 * card's earliest real printing). This file contributes only the PZ2-specific presentation row.
 */
val FracturedIdentityReprint = Printing(
    oracleId = "1515b0c2-1b55-4cd4-ad81-fb6b1f3e8188",
    name = "Fractured Identity",
    setCode = "PZ2",
    collectorNumber = "65681",
    scryfallId = "8b5a11be-59a8-4710-9417-be4f0b6d541a",
    artist = "Yongjae Choi",
    imageUri = "https://cards.scryfall.io/normal/front/8/b/8b5a11be-59a8-4710-9417-be4f0b6d541a.jpg?1783935592",
    releaseDate = "2017-11-15",
    rarity = Rarity.RARE,
)
