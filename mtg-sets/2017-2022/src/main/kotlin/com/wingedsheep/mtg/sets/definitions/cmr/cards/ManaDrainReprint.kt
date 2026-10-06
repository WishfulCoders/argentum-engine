package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mana Drain reprints in Commander Legends (CMR). Canonical CardDefinition lives in its earliest
 * set (LEG). CMR printed two collector numbers: the main #80 and the extended-art #637.
 */
val ManaDrainReprint = Printing(
    oracleId = "74d3277a-38e5-4732-afed-084a56148f20",
    name = "Mana Drain",
    setCode = "CMR",
    collectorNumber = "80",
    scryfallId = "ba874c0c-f66f-4edc-9859-40273487aef0",
    artist = "Raymond Swanland",
    imageUri = "https://cards.scryfall.io/normal/front/b/a/ba874c0c-f66f-4edc-9859-40273487aef0.jpg?1783928858",
    releaseDate = "2020-11-20",
    rarity = Rarity.MYTHIC,
)

val ManaDrainReprintExtended = Printing(
    oracleId = "74d3277a-38e5-4732-afed-084a56148f20",
    name = "Mana Drain",
    setCode = "CMR",
    collectorNumber = "637",
    scryfallId = "e351ec05-8c4d-4631-9dbf-7f85f664770b",
    artist = "Raymond Swanland",
    imageUri = "https://cards.scryfall.io/normal/front/e/3/e351ec05-8c4d-4631-9dbf-7f85f664770b.jpg?1783928623",
    releaseDate = "2020-11-20",
    rarity = Rarity.MYTHIC,
)
