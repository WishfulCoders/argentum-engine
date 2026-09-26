package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.dsl.Targets

/**
 * Pilfer
 * {1}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it.
 * That player discards that card.
 *
 * Canonical printing: Dominaria United (DMU) — the earliest real expansion printing.
 * The Foundations (FDN) reprint contributes only a `Printing` row.
 */
val Pilfer = card("Pilfer") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. That player discards that card."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = opponentHand,
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
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Pauline Voss"
        flavorText = "To the merchant, it was nothing more than a few missing trinkets. To Tinybones, it was the greatest heist of all time."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d872c10-4126-4130-a74a-1331ed418ca8.jpg?1782700494"
    }
}
