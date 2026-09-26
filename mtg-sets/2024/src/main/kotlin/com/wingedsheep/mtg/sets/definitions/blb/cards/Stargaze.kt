package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stargaze
 * {X}{B}{B}
 * Sorcery
 * Look at twice X cards from the top of your library. Put X cards from among them
 * into your hand and the rest into your graveyard. You lose X life.
 */
val Stargaze = card("Stargaze") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Look at twice X cards from the top of your library. Put X cards from among them into your hand and the rest into your graveyard. You lose X life."

    spell {
        effect = Effects.Pipeline {
            // Gather twice X cards from top of library
            val looked = gather(CardSource.TopOfLibrary(DynamicAmounts.xValue() * 2))
            // Select exactly X to keep
            val (kept, rest) = chooseExactlySplit(
                DynamicAmounts.xValue(),
                from = looked,
                selectedLabel = "Put in hand",
                remainderLabel = "Put in graveyard"
            )
            // Move selected to hand
            toHand(kept)
            // Move rest to graveyard
            toGraveyard(rest)
            // Lose X life
            run(Effects.LoseLife(DynamicAmounts.xValue(), EffectTarget.Controller))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Serena Malyon"
        flavorText = "\"Some batfolk dedicate their lives to seeing the world beyond them.\"\n—Warion, scholar of the Cosmos"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/777fc599-8de7-44d2-8fdd-9bddf5948a0c.jpg?1721426524"
    }
}
