package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Conditions

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns

/**
 * Thought-Stalker Warlock
 * {2}{B}
 * Creature — Lizard Warlock
 * 2/2
 *
 * Menace
 * When this creature enters, choose target opponent. If they lost life this turn,
 * they reveal their hand, you choose a nonland card from it, and they discard that card.
 * Otherwise, they discard a card.
 */
val ThoughtStalkerWarlock = card("Thought-Stalker Warlock") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Lizard Warlock"
    power = 2
    toughness = 2
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\nWhen this creature enters, choose target opponent. If they lost life this turn, they reveal their hand, you choose a nonland card from it, and they discard that card. Otherwise, they discard a card."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.If(
            // "If THEY lost life this turn" — bound to the chosen target opponent,
            // not any opponent (matters in multiplayer)
            condition = Conditions.PlayerLostLifeThisTurn(opponent.asPlayer),
            // If they lost life: reveal hand, controller chooses nonland, discard it
            then = Effects.Pipeline {
                run(Effects.RevealHand(opponent))
                val nonlandCards = gather(
                    CardSource.FromZone(Zone.HAND, opponent.asPlayer, GameObjectFilter.Nonland)
                )
                val chosenCard = chooseExactly(
                    1,
                    from = nonlandCards,
                    chooser = Chooser.Controller,
                    prompt = "Choose a nonland card to discard"
                )
                discard(chosenCard, opponent.asPlayer)
            },
            // Otherwise: they discard a card (their choice)
            otherwise = Patterns.Hand.discardCards(1, opponent)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42e80284-d489-493b-ae92-95b742d07cb3.jpg?1721426544"
        ruling("2024-07-26", "Thought-Stalker Warlock's triggered ability cares whether an opponent lost life this turn, not how their life total changed. For example, an opponent who gained 2 life and lost 1 life in the same turn still lost life.")
    }
}
