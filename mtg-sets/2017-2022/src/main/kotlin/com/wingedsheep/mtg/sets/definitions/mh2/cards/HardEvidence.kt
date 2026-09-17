package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hard Evidence
 * {U}
 * Sorcery
 * Create a 0/3 blue Crab creature token.
 * Investigate.
 */
val HardEvidence = card("Hard Evidence") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Create a 0/3 blue Crab creature token.\n" +
        "Investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 3,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Crab"),
            imageUri = "https://cards.scryfall.io/normal/front/7/e/7ef7f37a-b7f5-45a1-8f2b-7097089ca2e5.jpg?1783926593"
        ).then(Effects.Investigate())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Yeong-Hao Han"
        flavorText = "The investigator felt a pinch on his ankle. When he looked down, something glittered in the sand."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/501599d6-1072-4124-b05d-01f96de153f3.jpg?1783926878"
    }
}
