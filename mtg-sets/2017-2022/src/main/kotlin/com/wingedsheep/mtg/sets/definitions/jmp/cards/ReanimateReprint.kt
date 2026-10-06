package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Reanimate reprint in Jumpstart. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in TMP's `cards/` package (the card's earliest real printing); this file contributes
 * only the JMP presentation row.
 */
val ReanimateReprint = Printing(
    oracleId = "a044474a-cd72-4e9d-bd8d-a08f2de9cdc0",
    name = "Reanimate",
    setCode = "JMP",
    collectorNumber = "270",
    scryfallId = "652271a0-80e8-4b9b-8823-26c1528378fc",
    artist = "Johann Bodin",
    imageUri = "https://cards.scryfall.io/normal/front/6/5/652271a0-80e8-4b9b-8823-26c1528378fc.jpg?1783930411",
    releaseDate = "2020-07-17",
    rarity = Rarity.RARE,
)
