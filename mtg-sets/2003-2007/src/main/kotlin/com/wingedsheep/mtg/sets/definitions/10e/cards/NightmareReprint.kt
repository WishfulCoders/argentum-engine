package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Nightmare reprint in 10E. The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in
 * the card's earliest real printing; this file contributes only per-printing presentation data.
 */

val NightmareReprint = Printing(
    oracleId = "375932e6-1b3e-48dc-8154-9b664c3add34",
    name = "Nightmare",
    setCode = "10E",
    collectorNumber = "164",
    scryfallId = "88111e2b-84ff-48bf-9a64-b4e9022b6dae",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/8/8/88111e2b-84ff-48bf-9a64-b4e9022b6dae.jpg?1783943031",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

val NightmareReprintFoil = Printing(
    oracleId = "375932e6-1b3e-48dc-8154-9b664c3add34",
    name = "Nightmare",
    setCode = "10E",
    collectorNumber = "164★",
    scryfallId = "5d0001a4-18f9-460c-b26f-476ce55e6294",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/5/d/5d0001a4-18f9-460c-b26f-476ce55e6294.jpg?1783943031",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
