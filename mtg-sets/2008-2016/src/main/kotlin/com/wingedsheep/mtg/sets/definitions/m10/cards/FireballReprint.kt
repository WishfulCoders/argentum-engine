package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fireball reprint in M10. Canonical CardDefinition lives in Limited Edition Alpha (its
 * earliest real printing), `com.wingedsheep.mtg.sets.definitions.lea.cards.Fireball`.
 */
val FireballReprint = Printing(
    oracleId = "aa7714b0-2bfb-458a-8ebf-37ec2c53383e",
    name = "Fireball",
    setCode = "M10",
    collectorNumber = "136",
    scryfallId = "1dc84106-aec3-4e52-8dcb-1ed545bf3058",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1dc84106-aec3-4e52-8dcb-1ed545bf3058.jpg?1783942374",
    releaseDate = "2009-07-17",
    rarity = Rarity.UNCOMMON,
)
