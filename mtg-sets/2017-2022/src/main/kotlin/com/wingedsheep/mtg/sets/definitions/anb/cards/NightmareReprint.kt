package com.wingedsheep.mtg.sets.definitions.anb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Nightmare reprint in ANB. The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in
 * the card's earliest real printing; this file contributes only per-printing presentation data.
 */

val NightmareReprint = Printing(
    oracleId = "375932e6-1b3e-48dc-8154-9b664c3add34",
    name = "Nightmare",
    setCode = "ANB",
    collectorNumber = "54",
    scryfallId = "ee1871e0-623b-4543-8b3c-3e54137cfe43",
    artist = "Vance Kovacs",
    imageUri = "https://cards.scryfall.io/normal/front/e/e/ee1871e0-623b-4543-8b3c-3e54137cfe43.jpg?1783929819",
    releaseDate = "2020-08-13",
    rarity = Rarity.RARE,
)
