package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fireball reprint in DST. Canonical CardDefinition lives in Limited Edition Alpha (its
 * earliest real printing), `com.wingedsheep.mtg.sets.definitions.lea.cards.Fireball`.
 */
val FireballReprint = Printing(
    oracleId = "aa7714b0-2bfb-458a-8ebf-37ec2c53383e",
    name = "Fireball",
    setCode = "DST",
    collectorNumber = "60",
    scryfallId = "967465c4-619a-4407-bffc-ffadb89726cd",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/9/6/967465c4-619a-4407-bffc-ffadb89726cd.jpg?1783944439",
    releaseDate = "2004-02-06",
    rarity = Rarity.UNCOMMON,
)
