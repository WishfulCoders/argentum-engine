package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Clone reprint in TLE. The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in
 * the card's earliest real printing; this file contributes only per-printing presentation data.
 */

val CloneReprint = Printing(
    oracleId = "42226b87-0746-4ebf-9fd0-108d508462af",
    name = "Clone",
    setCode = "TLE",
    collectorNumber = "11",
    scryfallId = "3065a184-58f8-43d4-9d9f-ed0f266ec8c9",
    artist = "Viacom",
    imageUri = "https://cards.scryfall.io/normal/front/3/0/3065a184-58f8-43d4-9d9f-ed0f266ec8c9.jpg?1783904860",
    releaseDate = "2025-11-21",
    rarity = Rarity.MYTHIC,
    frameEffects = listOf("inverted"),
    isFullArt = true,
)
