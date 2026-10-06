package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Monkey Cage reprint in J22. Canonical CardDefinition lives in Mercadian Masques (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.mmq.cards.MonkeyCage`.
 */
val MonkeyCageReprint = Printing(
    oracleId = "33a11c01-89a6-446b-84da-e48fd1f27446",
    name = "Monkey Cage",
    setCode = "J22",
    collectorNumber = "787",
    scryfallId = "e97a43ed-f69a-435c-8ede-62e664f60ee0",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/e/9/e97a43ed-f69a-435c-8ede-62e664f60ee0.jpg?1783918803",
    releaseDate = "2022-12-02",
    rarity = Rarity.RARE,
)
