package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Appetite for Brains
 * {B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a card from it with mana value 4 or greater
 * and exile that card.
 *
 * Canonical AVR printing. Same RevealHand → Gather → Select → exile pipeline as Aggressive
 * Negotiations, filtered to mana value ≥ 4.
 */
val AppetiteForBrains = card("Appetite for Brains") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a card from it with mana value " +
        "4 or greater and exile that card."

    spell {
        val opponent = target(Targets.Opponent)

        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val mv4Plus = gather(
                CardSource.FromZone(
                    zone = Zone.HAND,
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Any.manaValueAtLeast(4),
                )
            )
            val chosenCard = chooseExactly(
                1,
                from = mv4Plus,
                chooser = Chooser.Controller,
                prompt = "Choose a card with mana value 4 or greater to exile"
            )
            exile(chosenCard, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Michael C. Hayes"
        flavorText = "Just as with a peach, the first bite is always the juiciest."
        imageUri =
            "https://cards.scryfall.io/normal/front/0/6/062ee892-cce7-42bd-97c7-032cec61faca.jpg?1783940710"
    }
}
