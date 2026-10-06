package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hero's Blade reprint in CMR. Canonical CardDefinition lives in Fate Reforged (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.frf.cards.HerosBlade`.
 */
val HerosBladeReprint = Printing(
    oracleId = "e6bcd25d-39b6-4619-8a27-9f3d87f4a17b",
    name = "Hero's Blade",
    setCode = "CMR",
    collectorNumber = "314",
    scryfallId = "f5e982c4-19ef-40d6-a807-fd16c31e63db",
    artist = "Aaron Miller",
    imageUri = "https://cards.scryfall.io/normal/front/f/5/f5e982c4-19ef-40d6-a807-fd16c31e63db.jpg?1783928757",
    releaseDate = "2020-11-20",
    rarity = Rarity.UNCOMMON,
)
