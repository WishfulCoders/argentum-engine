package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fireball reprint in LEB. Canonical CardDefinition lives in Limited Edition Alpha (its
 * earliest real printing), `com.wingedsheep.mtg.sets.definitions.lea.cards.Fireball`.
 */
val FireballReprint = Printing(
    oracleId = "aa7714b0-2bfb-458a-8ebf-37ec2c53383e",
    name = "Fireball",
    setCode = "LEB",
    collectorNumber = "150",
    scryfallId = "a285ab2e-836e-45b0-894e-574f733cf3db",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/a/2/a285ab2e-836e-45b0-894e-574f733cf3db.jpg?1783948625",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
