package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bitterblossom
 * {1}{B}
 * Kindred Enchantment — Faerie
 * At the beginning of your upkeep, you lose 1 life and create a 1/1 black Faerie Rogue creature
 * token with flying.
 *
 * The life loss is mandatory and not a payment: the token comes even at 0 life (2008-04-01
 * rulings), so it is one composite, not a cost.
 */
val Bitterblossom = card("Bitterblossom") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Kindred Enchantment — Faerie"
    oracleText = "At the beginning of your upkeep, you lose 1 life and create a 1/1 black Faerie Rogue creature token with flying."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.LoseLife(1, EffectTarget.Controller).then(
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Faerie", "Rogue"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/1/6/1666cae8-8750-4091-8e45-259e76268db9.jpg?1783942773",
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Rebecca Guay"
        flavorText = "In Lorwyn's brief evenings, the sun pauses at the horizon long enough for a certain species of violet to bloom with the fragrance of mischief."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/8145fed6-6b51-420a-84cf-4ea5e0aa1883.jpg?1783942795"
        ruling("2008-04-01", "The effect is mandatory. You'll lose 1 life even if you have only 1 life left.")
        ruling("2008-04-01", "The life loss isn't a payment. You'll get a token even if you had 0 life (and another effect is stopping you from losing the game).")
    }
}
