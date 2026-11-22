package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Clone reprint in Limited Edition Beta. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Limited Edition Alpha (`lea`) `cards/`
 * package; this file contributes only per-printing presentation data.
 */
val CloneReprint = Printing(
    oracleId = "42226b87-0746-4ebf-9fd0-108d508462af",
    name = "Clone",
    setCode = "LEB",
    collectorNumber = "52",
    scryfallId = "af53b5fc-c31a-4f26-93bf-0c45c1f4e1e5",
    artist = "Julie Baroh",
    imageUri = "https://cards.scryfall.io/normal/front/a/f/af53b5fc-c31a-4f26-93bf-0c45c1f4e1e5.jpg?1783948646",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
