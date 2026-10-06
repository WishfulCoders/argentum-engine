package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Heirloom Blade reprint in J22. Canonical CardDefinition lives in Commander 2017 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.c17.cards.HeirloomBlade`.
 */
val HeirloomBladeReprint = Printing(
    oracleId = "7c3a8766-e440-48c5-a561-197490efdba5",
    name = "Heirloom Blade",
    setCode = "J22",
    collectorNumber = "775",
    scryfallId = "30a1fdeb-3569-4c2e-81ab-f746e08527eb",
    artist = "Carmen Sinek",
    imageUri = "https://cards.scryfall.io/normal/front/3/0/30a1fdeb-3569-4c2e-81ab-f746e08527eb.jpg?1783918809",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)
