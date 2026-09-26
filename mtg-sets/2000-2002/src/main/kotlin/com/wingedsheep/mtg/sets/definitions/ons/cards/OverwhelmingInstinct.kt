package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.Rarity

/**
 * Overwhelming Instinct
 * {2}{G}
 * Enchantment
 * Whenever you attack with three or more creatures, draw a card.
 */
val OverwhelmingInstinct = card("Overwhelming Instinct") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever you attack with three or more creatures, draw a card."

    triggeredAbility {
        trigger = Triggers.you.attacks(minAttackers = 3)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "276"
        artist = "Ron Spears"
        flavorText = "\"The biggest difference between a victory and a massacre is which side you're on.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d9e3793-7ddc-45c5-b25d-acd5cb96026f.jpg"
    }
}
