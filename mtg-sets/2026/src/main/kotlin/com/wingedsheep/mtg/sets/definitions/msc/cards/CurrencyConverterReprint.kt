package com.wingedsheep.mtg.sets.definitions.msc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Currency Converter reprints in Marvel Super Heroes Commander. Canonical [com.wingedsheep.sdk.model.CardDefinition] lives in its
 * earliest set's `cards/` package; these rows contribute only per-printing presentation data.
 */
val CurrencyConverterReprint = Printing(
    oracleId = "981298e6-ddee-49c0-9377-f47f019b4138",
    name = "Currency Converter",
    setCode = "MSC",
    collectorNumber = "197",
    scryfallId = "4eab054c-93f8-4da4-afb9-8f4c73c19213",
    artist = "N\u00e9stor Ossand\u00f3n Leal",
    imageUri = "https://cards.scryfall.io/normal/front/4/e/4eab054c-93f8-4da4-afb9-8f4c73c19213.jpg?1783903220",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
)

val CurrencyConverterVariantReprint = Printing(
    oracleId = "981298e6-ddee-49c0-9377-f47f019b4138",
    name = "Currency Converter",
    setCode = "MSC",
    collectorNumber = "430",
    scryfallId = "adff49ac-cb3c-4888-85b9-7eb07553e716",
    artist = "N\u00e9stor Ossand\u00f3n Leal",
    imageUri = "https://cards.scryfall.io/normal/front/a/d/adff49ac-cb3c-4888-85b9-7eb07553e716.jpg?1783903136",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
)
