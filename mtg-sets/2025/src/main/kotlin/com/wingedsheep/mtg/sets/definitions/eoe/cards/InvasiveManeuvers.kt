package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invasive Maneuvers
 * {1}{R}
 * Instant
 *
 * Invasive Maneuvers deals 3 damage to target creature. It deals 5 damage instead if you control a Spacecraft.
 */
val InvasiveManeuvers = card("Invasive Maneuvers") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Invasive Maneuvers deals 3 damage to target creature. It deals 5 damage instead if you control a Spacecraft."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Permanent.withSubtype("Spacecraft")),
            then = Effects.DealDamage(5, creature),
            otherwise = Effects.DealDamage(3, creature)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Leon Tukker"
        flavorText = "\"It saves a lot on ammunition. No, I'm not kidding.\"\n—Captain Mayav, Kavaron Memorial Navy"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b68010e2-7810-43cb-a52e-e73b1834e54e.jpg?1752947108"
    }
}
