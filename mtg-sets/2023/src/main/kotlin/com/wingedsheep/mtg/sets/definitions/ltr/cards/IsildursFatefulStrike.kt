package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Isildur's Fateful Strike
 * {2}{B}{B}
 * Legendary Instant
 * (You may cast a legendary instant only if you control a legendary creature or planeswalker.)
 * Destroy target creature. If its controller has more than four cards in hand, they exile
 * cards from their hand equal to the difference.
 */
val IsildursFatefulStrike = card("Isildur's Fateful Strike") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Instant"
    oracleText = "(You may cast a legendary instant only if you control a legendary creature or planeswalker.)\n" +
        "Destroy target creature. If its controller has more than four cards in hand, they exile cards from their hand equal to the difference."

    spell {
        val creature = target(TargetFilter.Creature)
        val controller = Player.ControllerOf("target creature")

        // Number of cards to exile = (controller's hand size − 4), but never negative.
        val excess = DynamicAmounts.nonNegative(
            DynamicAmounts.count(controller, Zone.HAND) - 4
        )

        effect = Effects.Pipeline {
            run(Effects.Move(creature, Zone.GRAVEYARD, byDestruction = true))
            val controllerHand = gather(CardSource.FromZone(Zone.HAND, controller))
            val exiled = chooseExactly(
                excess,
                from = controllerHand,
                chooser = Chooser.TargetPlayer,
                prompt = "Choose cards to exile from your hand"
            )
            move(exiled, CardDestination.ToZone(Zone.EXILE, controller))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "John Di Giovanni"
        flavorText = "Isildur cut the Ring from Sauron's hand with the hilt-shard of his father's sword, and Sauron himself was overthrown."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bf08071-fbf8-463e-9a57-59dbf0280dd8.jpg?1686968531"
    }
}
