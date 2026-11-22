package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Nightmare reprint in Limited Edition Beta. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Limited Edition Alpha (`lea`) `cards/`
 * package; this file contributes only per-printing presentation data.
 */
val NightmareReprint = Printing(
    oracleId = "375932e6-1b3e-48dc-8154-9b664c3add34",
    name = "Nightmare",
    setCode = "LEB",
    collectorNumber = "119",
    scryfallId = "fc78dced-27d2-441a-b63b-32356bc33747",
    artist = "Melissa A. Benson",
    imageUri = "https://cards.scryfall.io/normal/front/f/c/fc78dced-27d2-441a-b63b-32356bc33747.jpg?1783948632",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
