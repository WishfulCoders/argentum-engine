package com.wingedsheep.mtg.sets.definitions.msc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Skullclamp reprints in Marvel Super Heroes Commander. Canonical [com.wingedsheep.sdk.model.CardDefinition] lives in its
 * earliest set's `cards/` package; these rows contribute only per-printing presentation data.
 */
val SkullclampReprint = Printing(
    oracleId = "65986c1b-8e51-4604-b685-d82fa7d1263a",
    name = "Skullclamp",
    setCode = "MSC",
    collectorNumber = "210",
    scryfallId = "1d8b007b-3169-4ee3-80c7-781fc096fc7a",
    artist = "Lordigan",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d8b007b-3169-4ee3-80c7-781fc096fc7a.jpg?1783903215",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
)

val SkullclampVariantReprint = Printing(
    oracleId = "65986c1b-8e51-4604-b685-d82fa7d1263a",
    name = "Skullclamp",
    setCode = "MSC",
    collectorNumber = "451",
    scryfallId = "a740d70c-b877-44cf-beb4-774ec66236c5",
    artist = "Lordigan",
    imageUri = "https://cards.scryfall.io/normal/front/a/7/a740d70c-b877-44cf-beb4-774ec66236c5.jpg?1783903128",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
)
