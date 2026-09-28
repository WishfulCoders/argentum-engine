package com.wingedsheep.mtg.sets.definitions.msc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Toxic Deluge reprint in Marvel Super Heroes Commander. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in C13's `cards/` package (the card's earliest real printing); this file contributes
 * only the MSC presentation row.
 */
val ToxicDelugeReprint = Printing(
    oracleId = "afaef788-34d1-460b-b884-9d7ae6ddeb18",
    name = "Toxic Deluge",
    setCode = "MSC",
    collectorNumber = "161",
    scryfallId = "de5afccc-8d42-4bd6-b068-b9ea2361655e",
    artist = "Anthony Devine",
    imageUri = "https://cards.scryfall.io/normal/front/d/e/de5afccc-8d42-4bd6-b068-b9ea2361655e.jpg?1783903236",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
)
