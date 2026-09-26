package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jaya's Greeting
 * {1}{R}
 * Instant
 * Jaya's Greeting deals 3 damage to target creature. Scry 1.
 *
 * Two sentences, two effects: the damage against the bound target, then the controller's scry.
 */
val JayasGreeting = card("Jaya's Greeting") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Jaya's Greeting deals 3 damage to target creature. Scry 1."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(3, t) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Victor Adame Minguez"
        flavorText = "\"We have visitors? Well, it'd be rude not to give them a traditional Keral Keep welcome.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec66f169-5cf9-4d7c-a5ab-c64fc4801358.jpg"
    }
}
