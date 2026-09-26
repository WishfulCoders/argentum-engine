package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.dsl.Targets

/**
 * Addle
 * {1}{B}
 * Sorcery
 * Choose a color. Target player reveals their hand and you choose a card of that
 * color from it. That player discards that card.
 */
val Addle = card("Addle") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a color. Target player reveals their hand and you choose a card of " +
        "that color from it. That player discards that card."

    spell {
        val targetPlayer = target(Targets.Player)
        effect = Effects.ChooseColorThen(
            then = Effects.Pipeline {
                run(Effects.RevealHand(targetPlayer))
                val targetHand = gather(CardSource.FromZone(Zone.HAND, targetPlayer.asPlayer))
                val toDiscard = chooseExactly(
                    1,
                    from = targetHand,
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter(
                        cardPredicates = listOf(CardPredicate.HasChosenColor),
                    ),
                    prompt = "Choose a card of the chosen color to discard",
                    alwaysPrompt = true,
                    showAllCards = true
                )
                discard(toDiscard, targetPlayer.asPlayer)
            },
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8afb9d0-affa-4599-bf29-729cfe64703b.jpg?1562941705"
    }
}
