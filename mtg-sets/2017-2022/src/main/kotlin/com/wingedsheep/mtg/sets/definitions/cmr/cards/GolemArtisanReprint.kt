package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Golem Artisan reprint in CMR. Canonical CardDefinition lives in Scars of Mirrodin (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.som.cards.GolemArtisan`.
 */
val GolemArtisanReprint = Printing(
    oracleId = "8537fc2c-c027-4440-bff4-d046fc198428",
    name = "Golem Artisan",
    setCode = "CMR",
    collectorNumber = "311",
    scryfallId = "441a3345-2507-46ed-bdb3-c5c45d17da51",
    artist = "Nic Klein",
    imageUri = "https://cards.scryfall.io/normal/front/4/4/441a3345-2507-46ed-bdb3-c5c45d17da51.jpg?1783928761",
    releaseDate = "2020-11-20",
    rarity = Rarity.UNCOMMON,
)
