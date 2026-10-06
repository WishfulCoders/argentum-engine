package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Golem Artisan reprint in J22. Canonical CardDefinition lives in Scars of Mirrodin (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.som.cards.GolemArtisan`.
 */
val GolemArtisanReprint = Printing(
    oracleId = "8537fc2c-c027-4440-bff4-d046fc198428",
    name = "Golem Artisan",
    setCode = "J22",
    collectorNumber = "770",
    scryfallId = "3a673d71-58a5-4cd4-b9bc-d7819269d693",
    artist = "Nic Klein",
    imageUri = "https://cards.scryfall.io/normal/front/3/a/3a673d71-58a5-4cd4-b9bc-d7819269d693.jpg?1783918809",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)
