package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Control of the Court
 * {1}{R}
 * Sorcery
 * Draw four cards, then discard three cards at random.
 */
val ControlOfTheCourt = card("Control of the Court") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Draw four cards, then discard three cards at random."

    spell {
        effect = Effects.DrawCards(4) then Patterns.Hand.discardRandom(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Li Yousong"
        flavorText = "\"Power-hungry eunuchs, the curse of the dynasty, have thrown the masses of the people into the depths of misery.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e4f0005-4f19-4352-9cd2-3993ae6db879.jpg?1783946108"
    }
}
