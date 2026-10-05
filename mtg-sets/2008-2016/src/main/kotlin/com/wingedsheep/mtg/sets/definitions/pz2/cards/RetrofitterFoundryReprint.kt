package com.wingedsheep.mtg.sets.definitions.pz2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Retrofitter Foundry reprint in Treasure Chest. Canonical [com.wingedsheep.sdk.model.CardDefinition] lives in its
 * earliest set's `cards/` package; this row contributes only per-printing presentation data.
 */
val RetrofitterFoundryReprint = Printing(
    oracleId = "aaadfe41-b2be-4183-b45e-a70e53a59d2e",
    name = "Retrofitter Foundry",
    setCode = "PZ2",
    collectorNumber = "70697",
    scryfallId = "775fd8e8-80bb-4818-9277-b9d211c3a00e",
    artist = "Dmitry Burmak",
    imageUri = "https://cards.scryfall.io/normal/front/7/7/775fd8e8-80bb-4818-9277-b9d211c3a00e.jpg?1783933943",
    releaseDate = "2018-12-06",
    rarity = Rarity.RARE,
)
