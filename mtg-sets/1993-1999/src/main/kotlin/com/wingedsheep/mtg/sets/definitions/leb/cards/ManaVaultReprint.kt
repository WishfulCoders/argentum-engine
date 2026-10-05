package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mana Vault reprint in Limited Edition Beta. Canonical [com.wingedsheep.sdk.model.CardDefinition] lives in its
 * earliest set's `cards/` package; this row contributes only per-printing presentation data.
 */
val ManaVaultReprint = Printing(
    oracleId = "736892cb-a34b-4bb9-b56c-e26e3db207a2",
    name = "Mana Vault",
    setCode = "LEB",
    collectorNumber = "260",
    scryfallId = "a11f55e8-7f86-4ca9-b737-9a920d9cf282",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/a/1/a11f55e8-7f86-4ca9-b737-9a920d9cf282.jpg?1783948602",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
