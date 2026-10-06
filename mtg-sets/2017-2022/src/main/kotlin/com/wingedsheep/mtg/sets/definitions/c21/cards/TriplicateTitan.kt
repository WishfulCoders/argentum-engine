package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Triplicate Titan
 * {9}
 * Artifact Creature — Golem
 * 9/9
 * Flying, vigilance, trample
 * When this creature dies, create a 3/3 colorless Golem artifact creature token with flying, a 3/3
 * colorless Golem artifact creature token with vigilance, and a 3/3 colorless Golem artifact creature
 * token with trample.
 *
 * The dies trigger makes three distinct tokens, one keyword each — three [Effects.CreateToken]s in
 * one resolution, each with its own C21 token art.
 */
val TriplicateTitan = card("Triplicate Titan") {
    manaCost = "{9}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 9
    toughness = 9
    oracleText = "Flying, vigilance, trample\n" +
        "When this creature dies, create a 3/3 colorless Golem artifact creature token with flying, " +
        "a 3/3 colorless Golem artifact creature token with vigilance, and a 3/3 colorless Golem " +
        "artifact creature token with trample."

    keywords(Keyword.FLYING, Keyword.VIGILANCE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Golem"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/c/6/c6a3a35a-ebd8-47e5-a5ed-c736b8bed968.jpg?1783927196",
        ) then Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Golem"),
            keywords = setOf(Keyword.VIGILANCE),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/f/2/f27efb56-7fd4-4d0e-b641-a152b3ef8953.jpg?1783927195",
        ) then Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Golem"),
            keywords = setOf(Keyword.TRAMPLE),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/4/0/40c50c6d-5116-4acb-89d3-f27efb20d336.jpg?1783927195",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/568dd5a6-86f4-4039-87d7-812120c8ab94.jpg?1783927584"
    }
}
