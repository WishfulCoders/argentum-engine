package com.wingedsheep.mtg.sets.definitions.ddp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Oust reprint in DDP. The canonical CardDefinition lives in
 * Rise of the Eldrazi (`roe`), the card's earliest real printing; this file
 * contributes only per-printing presentation data.
 */
val OustReprint = Printing(
    oracleId = "efc12fda-054b-466a-a863-06cf54878172",
    name = "Oust",
    setCode = "DDP",
    collectorNumber = "7",
    scryfallId = "fd915d09-5ecf-45b6-90ae-970a2c7de475",
    artist = "Mike Bierek",
    imageUri = "https://cards.scryfall.io/normal/front/f/d/fd915d09-5ecf-45b6-90ae-970a2c7de475.jpg?1783938260",
    releaseDate = "2015-08-28",
    rarity = Rarity.UNCOMMON,
)
