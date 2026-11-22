package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Evil Presence reprint in Limited Edition Beta. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Limited Edition Alpha (`lea`) `cards/`
 * package; this file contributes only per-printing presentation data.
 */
val EvilPresenceReprint = Printing(
    oracleId = "3d8ac41c-0566-48b2-a744-39db2f72272c",
    name = "Evil Presence",
    setCode = "LEB",
    collectorNumber = "108",
    scryfallId = "9e995f4b-efd3-4ac7-8fec-adb913294815",
    artist = "Sandra Everingham",
    imageUri = "https://cards.scryfall.io/normal/front/9/e/9e995f4b-efd3-4ac7-8fec-adb913294815.jpg?1783948633",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
