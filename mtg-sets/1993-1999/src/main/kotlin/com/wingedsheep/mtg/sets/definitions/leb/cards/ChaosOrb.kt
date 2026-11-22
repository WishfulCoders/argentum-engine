package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Chaos Orb reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ChaosOrbReprint = Printing(
    oracleId = "edb455f4-8dc9-4b7c-b25c-cb51b7dfbb41",
    name = "Chaos Orb",
    setCode = "LEB",
    collectorNumber = "236",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/6/b/6bec436c-2869-432a-b3cf-633a58af6d4c.jpg?1783948607",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
