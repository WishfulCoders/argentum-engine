package com.wingedsheep.mtg.sets.definitions.msc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hero's Blade reprint in MSC. Canonical CardDefinition lives in Fate Reforged (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.frf.cards.HerosBlade`.
 */
val HerosBladeReprint = Printing(
    oracleId = "e6bcd25d-39b6-4619-8a27-9f3d87f4a17b",
    name = "Hero's Blade",
    setCode = "MSC",
    collectorNumber = "201",
    scryfallId = "6f2fa911-5735-4ee2-828f-59dea6927b20",
    artist = "Jason Smith",
    imageUri = "https://cards.scryfall.io/normal/front/6/f/6f2fa911-5735-4ee2-828f-59dea6927b20.jpg?1783903219",
    releaseDate = "2026-06-26",
    rarity = Rarity.UNCOMMON,
)
