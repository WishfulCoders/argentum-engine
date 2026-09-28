package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Toxic Deluge reprint in Modern Horizons 3. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in C13's `cards/` package (the card's earliest real printing); this file contributes
 * only the MH3 presentation row.
 */
val ToxicDelugeReprint = Printing(
    oracleId = "afaef788-34d1-460b-b884-9d7ae6ddeb18",
    name = "Toxic Deluge",
    setCode = "MH3",
    collectorNumber = "277",
    scryfallId = "5aa02b7d-db31-4924-b75e-eb02f332ca3a",
    artist = "Svetlin Velinov",
    imageUri = "https://cards.scryfall.io/normal/front/5/a/5aa02b7d-db31-4924-b75e-eb02f332ca3a.jpg?1783911214",
    releaseDate = "2024-06-14",
    rarity = Rarity.RARE,
)
