package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hero's Blade reprint in J22. Canonical CardDefinition lives in Fate Reforged (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.frf.cards.HerosBlade`.
 */
val HerosBladeReprint = Printing(
    oracleId = "e6bcd25d-39b6-4619-8a27-9f3d87f4a17b",
    name = "Hero's Blade",
    setCode = "J22",
    collectorNumber = "776",
    scryfallId = "b342fb32-d1a0-4fb3-9765-af7674bc6628",
    artist = "Aaron Miller",
    imageUri = "https://cards.scryfall.io/normal/front/b/3/b342fb32-d1a0-4fb3-9765-af7674bc6628.jpg?1783918808",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)
