package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fireball reprint in CLB, in both the regular and the showcase collector number. Canonical CardDefinition lives in Limited Edition Alpha (its
 * earliest real printing), `com.wingedsheep.mtg.sets.definitions.lea.cards.Fireball`.
 */
val FireballReprint = Printing(
    oracleId = "aa7714b0-2bfb-458a-8ebf-37ec2c53383e",
    name = "Fireball",
    setCode = "CLB",
    collectorNumber = "175",
    scryfallId = "df45a43e-a5b7-4fd4-873b-7b3c021be198",
    artist = "Xavier Ribeiro",
    imageUri = "https://cards.scryfall.io/normal/front/d/f/df45a43e-a5b7-4fd4-873b-7b3c021be198.jpg?1783922739",
    releaseDate = "2022-06-10",
    rarity = Rarity.UNCOMMON,
)

val FireballReprintB = Printing(
    oracleId = "aa7714b0-2bfb-458a-8ebf-37ec2c53383e",
    name = "Fireball",
    setCode = "CLB",
    collectorNumber = "397",
    scryfallId = "82a97aae-d8a7-4451-a47e-ea878dea6e4c",
    artist = "Justine Jones",
    imageUri = "https://cards.scryfall.io/normal/front/8/2/82a97aae-d8a7-4451-a47e-ea878dea6e4c.jpg?1783922638",
    releaseDate = "2022-06-10",
    rarity = Rarity.UNCOMMON,
    frameEffects = listOf("showcase"),
)
