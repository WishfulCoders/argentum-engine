package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.dsl.Targets

val StingingVitriol = card("Stinging Vitriol") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Stinging Vitriol deals 2 damage to target opponent. That player reveals their hand. " +
        "You choose a nonland card from it. They discard that card."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.DealDamage(2, opponent))
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Nathaniel Himawan"
        flavorText = "\"Confidence is relative. Bolster yours by breaking theirs.\"\n—Ingris Stingerquill"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7d78297-7411-4ec5-8931-a25146869d5b.jpg?1789385971"
        inBooster = false
    }
}
