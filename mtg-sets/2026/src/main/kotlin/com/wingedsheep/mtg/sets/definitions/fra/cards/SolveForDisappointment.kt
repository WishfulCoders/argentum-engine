package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.dsl.Targets

val SolveForDisappointment = card("Solve for Disappointment") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland permanent card from it. That player discards that card.\n" +
        "Empower Jace 1. (Put a loyalty counter on a Jace token you control. If you don't control one, first create a blue Jace planeswalker token with \"[−1]: Surveil 1\" and \"[−3]: Draw a card.\")"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.NonlandPermanent,
                prompt = "Choose a nonland permanent card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
            run(Patterns.Mechanic.empowerJace(1))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Eli Minaya"
        flavorText = "The Theorist has no interest in failures."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7beaa8c9-1a2c-4c88-b579-91e371d8d9e3.jpg?1788878155"
        inBooster = false
    }
}
