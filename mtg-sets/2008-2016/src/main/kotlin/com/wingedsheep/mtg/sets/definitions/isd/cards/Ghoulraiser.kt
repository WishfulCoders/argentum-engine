package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ghoulraiser
 * {1}{B}{B}
 * Creature — Zombie
 * 2/2
 * When this creature enters, return a Zombie card at random from your graveyard to your hand.
 */
val Ghoulraiser = card("Ghoulraiser") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText =
        "When this creature enters, return a Zombie card at random from your graveyard to your hand."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val zombies = gather(
                CardSource.FromZone(
                    Zone.GRAVEYARD,
                    Player.You,
                    GameObjectFilter.Any.withSubtype(Subtype.ZOMBIE),
                )
            )
            val chosen = chooseRandom(1, from = zombies)
            toHand(chosen)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Steve Prescott"
        flavorText =
            "\"Come. Bring your brothers. Tonight, you feast on living flesh.\"\n—Jadar, ghoulcaller of Nephalia"
        imageUri =
            "https://cards.scryfall.io/normal/front/5/2/52c537d1-2d57-4a87-9dac-594d40d95633.jpg?1783940954"
    }
}
