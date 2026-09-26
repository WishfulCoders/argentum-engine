package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hatching Plans
 * {1}{U}
 * Enchantment
 * When this enchantment is put into a graveyard from the battlefield, draw three cards.
 */
val HatchingPlans = card("Hatching Plans") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment is put into a graveyard from the battlefield, draw three cards."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Heather Hudson"
        flavorText = "So wondrous to behold, so delicate and finely crafted—and yet, such a pleasure to smash."
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef327629-9831-4a0c-bd61-7542b0713ea8.jpg?1783943522"
    }
}
