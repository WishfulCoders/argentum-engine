package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Phantasmal Terrain reprint in Limited Edition Beta. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Limited Edition Alpha (`lea`) `cards/`
 * package; this file contributes only per-printing presentation data.
 */
val PhantasmalTerrainReprint = Printing(
    oracleId = "7dcbce46-2973-4a9f-93df-95ac41ce668a",
    name = "Phantasmal Terrain",
    setCode = "LEB",
    collectorNumber = "69",
    scryfallId = "9c29369c-d909-45a7-be70-3181ddac9728",
    artist = "Dameon Willich",
    imageUri = "https://cards.scryfall.io/normal/front/9/c/9c29369c-d909-45a7-be70-3181ddac9728.jpg?1783948642",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
