package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Explorer's Scope reprint in CMR. Canonical CardDefinition lives in Zendikar (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.zen.cards.ExplorersScope`.
 */
val ExplorersScopeReprint = Printing(
    oracleId = "a563ede9-b92f-4285-88f8-abcbdd017742",
    name = "Explorer's Scope",
    setCode = "CMR",
    collectorNumber = "461",
    scryfallId = "b16cf33f-ef8d-4d10-87fe-ab03b8525766",
    artist = "Vincent Proce",
    imageUri = "https://cards.scryfall.io/normal/front/b/1/b16cf33f-ef8d-4d10-87fe-ab03b8525766.jpg?1783928692",
    releaseDate = "2020-11-20",
    rarity = Rarity.COMMON,
)
