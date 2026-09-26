package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Urgoros, the Empty One
 * {4}{B}{B}
 * Legendary Creature — Specter
 * 4/3
 * Flying
 * Whenever Urgoros, the Empty One deals combat damage to a player, that player
 * discards a card at random. If the player can't, you draw a card.
 */
val UrgorosTheEmptyOne = card("Urgoros, the Empty One") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Specter"
    power = 4
    toughness = 3
    oracleText = "Flying\nWhenever Urgoros, the Empty One deals combat damage to a player, that player discards a card at random. If the player can't, you draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            // Gather the damaged player's hand
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.TriggeringPlayer))
            // If hand not empty, discard one at random; if empty, controller draws
            ifNotEmpty(hand) {
                val discarded = chooseRandom(1, from = hand)
                discard(discarded, Player.TriggeringPlayer)
            } orElse {
                run(Effects.DrawCards(count = 1))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Daarken"
        flavorText = "As the phantom flies overhead, the city of Vhelnish screams in silence."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8652fb7-1bce-44ee-b75e-4c773ff8fdb3.jpg?1562744748"
        ruling("2018-04-27", "If that player has one card in hand, it's discarded at random (even though that's not very random). You won't draw a card.")
    }
}
