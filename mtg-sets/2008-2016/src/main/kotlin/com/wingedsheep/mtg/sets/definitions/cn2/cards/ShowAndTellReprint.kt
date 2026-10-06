package com.wingedsheep.mtg.sets.definitions.cn2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Show and Tell reprint in Conspiracy: Take the Crown. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Urza's Saga (`usg`) `cards/` package;
 * this file contributes only per-printing presentation data.
 */
val ShowAndTellReprint = Printing(
    oracleId = "b83a3ba0-249e-4c39-bbf0-cb005413f7d2",
    name = "Show and Tell",
    setCode = "CN2",
    collectorNumber = "121",
    scryfallId = "fa7b7897-36e0-415a-8bb7-602886164852",
    artist = "Zack Stella",
    imageUri = "https://cards.scryfall.io/normal/front/f/a/fa7b7897-36e0-415a-8bb7-602886164852.jpg?1783937320",
    releaseDate = "2016-08-26",
    rarity = Rarity.MYTHIC,
    borderColor = "black",
)
