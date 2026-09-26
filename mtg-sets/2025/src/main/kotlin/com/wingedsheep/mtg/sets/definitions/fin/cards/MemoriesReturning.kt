package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Memories Returning
 * {2}{U}{U}
 * Sorcery
 * Reveal the top five cards of your library. Put one of them into your hand. Then choose an
 * opponent. They put one on the bottom of your library. Then you put one into your hand. Then
 * they put one on the bottom of your library. Put the other into your hand.
 * Flashback {7}{U}{U}
 *
 * Modeled as a strict alternation of single picks over one revealed pile, with the remainder of
 * each selection feeding the next, so the five revealed cards are partitioned exactly: three you
 * choose go to your hand, two an opponent chooses go to the bottom of your library, and the final
 * leftover goes to your hand. The opponent is the sole opponent ([Chooser.Opponent]); in a
 * two-player game "choose an opponent" is unambiguous.
 */
val MemoriesReturning = card("Memories Returning") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Reveal the top five cards of your library. Put one of them into your hand. Then " +
        "choose an opponent. They put one on the bottom of your library. Then you put one into " +
        "your hand. Then they put one on the bottom of your library. Put the other into your hand.\n" +
        "Flashback {7}{U}{U}"

    val toBottom = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)

    spell {
        effect = Effects.Pipeline {
            fun pickOne(from: CollectionSlot, chooser: Chooser, prompt: String) =
                chooseExactlySplit(1, from = from, chooser = chooser, showAllCards = true, alwaysPrompt = true, prompt = prompt)

            // Reveal the top five cards.
            val revealed = gather(CardSource.TopOfLibrary(5))
            reveal(revealed)
            // You put one of them into your hand.
            val (hand1, rem1) = pickOne(revealed, Chooser.Controller, "Put a card into your hand")
            toHand(hand1)
            // An opponent puts one on the bottom of your library.
            val (bottom1, rem2) = pickOne(rem1, Chooser.Opponent, "An opponent puts a card on the bottom of your library")
            move(bottom1, toBottom)
            // You put one into your hand.
            val (hand2, rem3) = pickOne(rem2, Chooser.Controller, "Put a card into your hand")
            toHand(hand2)
            // An opponent puts one on the bottom of your library.
            val (bottom2, rem4) = pickOne(rem3, Chooser.Opponent, "An opponent puts a card on the bottom of your library")
            move(bottom2, toBottom)
            // Put the other (the last remaining card) into your hand.
            toHand(rem4)
        }
    }

    keywordAbility(KeywordAbility.flashback("{7}{U}{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Grace Zhu"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a753abfc-35d3-4faf-ab35-3b51aa778174.jpg?1748705992"
    }
}
