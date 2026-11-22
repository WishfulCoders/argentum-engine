package com.wingedsheep.mtg.sets.definitions.arc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fireball reprint in ARC. Canonical CardDefinition lives in Limited Edition Alpha (its
 * earliest real printing), `com.wingedsheep.mtg.sets.definitions.lea.cards.Fireball`.
 */
val FireballReprint = Printing(
    oracleId = "aa7714b0-2bfb-458a-8ebf-37ec2c53383e",
    name = "Fireball",
    setCode = "ARC",
    collectorNumber = "37",
    scryfallId = "c463b755-cdb9-49c5-a458-50ec1fac8b3c",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/c/4/c463b755-cdb9-49c5-a458-50ec1fac8b3c.jpg?1783941908",
    releaseDate = "2010-06-18",
    rarity = Rarity.UNCOMMON,
)
