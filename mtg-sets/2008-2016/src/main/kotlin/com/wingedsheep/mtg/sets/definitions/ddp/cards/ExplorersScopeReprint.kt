package com.wingedsheep.mtg.sets.definitions.ddp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Explorer's Scope reprint in DDP. Canonical CardDefinition lives in Zendikar (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.zen.cards.ExplorersScope`.
 */
val ExplorersScopeReprint = Printing(
    oracleId = "a563ede9-b92f-4285-88f8-abcbdd017742",
    name = "Explorer's Scope",
    setCode = "DDP",
    collectorNumber = "28",
    scryfallId = "2087a2bb-7448-44c8-a075-0da6a5cdc418",
    artist = "Vincent Proce",
    imageUri = "https://cards.scryfall.io/normal/front/2/0/2087a2bb-7448-44c8-a075-0da6a5cdc418.jpg?1783938253",
    releaseDate = "2015-08-28",
    rarity = Rarity.COMMON,
)
