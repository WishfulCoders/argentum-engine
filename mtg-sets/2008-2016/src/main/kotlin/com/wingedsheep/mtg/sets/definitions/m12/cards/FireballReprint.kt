package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fireball reprint in M12. Canonical CardDefinition lives in Limited Edition Alpha (its
 * earliest real printing), `com.wingedsheep.mtg.sets.definitions.lea.cards.Fireball`.
 */
val FireballReprint = Printing(
    oracleId = "aa7714b0-2bfb-458a-8ebf-37ec2c53383e",
    name = "Fireball",
    setCode = "M12",
    collectorNumber = "131",
    scryfallId = "f6a86f5d-cfbf-4d8e-9f8e-0f8288907396",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/f/6/f6a86f5d-cfbf-4d8e-9f8e-0f8288907396.jpg?1783941072",
    releaseDate = "2011-07-15",
    rarity = Rarity.UNCOMMON,
)
