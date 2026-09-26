package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Cabal Conditioning
 * {6}{B}
 * Sorcery
 * Any number of target players each discard a number of cards equal to the greatest
 * mana value among permanents you control.
 */
val CabalConditioning = card("Cabal Conditioning") {
    manaCost = "{6}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Any number of target players each discard a number of cards equal to the greatest mana value among permanents you control."

    spell {
        target(TargetPlayer(count = 2, optional = true))
        effect = Effects.ForEachTarget(
            Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)))
            val discarded = chooseExactly(
                DynamicAmounts.battlefield(Player.You).maxManaValue(),
                from = hand,
                chooser = Chooser.TargetPlayer
            )
            discard(discarded, Player.ContextPlayer(0))
        }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Scott M. Fischer"
        flavorText = "\"Hear only the Cabal's voice. See only the Cabal's way. Speak only the Cabal's word.\" —Cabal mantra"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb81c6e6-fded-4cd3-a6fa-486419a5408a.jpg?1650357097"
    }
}
