package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * The Modern Age // Vector Glider
 * {1}{U} — Enchantment — Saga // Enchantment Creature — Spirit 2/3
 *
 * Front — The Modern Age:
 *   I, II — Draw a card, then discard a card.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Vector Glider:
 *   Flying
 */
private val VectorGlider = card("Vector Glider") {
    manaCost = ""
    colorIndicator = "U"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Spirit"
    oracleText = "Flying"
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Anastasia Ovchinnikova"
        flavorText = "It drifted from the spirit realm into a neon world full of new possibilities . . . " +
            "and decided to stay."
        imageUri = "https://cards.scryfall.io/normal/back/e/e/ee60a8e9-3201-4a7e-8aa4-2c5e1042c8a5.jpg?1783923907"
    }
}

private val TheModernAgeFront = card("The Modern Age") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — Draw a card, then discard a card.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    for (chapter in 1..2) {
        sagaChapter(chapter) {
            effect = Patterns.Hand.loot()
        }
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee60a8e9-3201-4a7e-8aa4-2c5e1042c8a5.jpg?1783923907"
    }
}

val TheModernAge: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TheModernAgeFront,
    backFace = VectorGlider,
)
