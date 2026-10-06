package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Expressive Iteration
 * {U}{R}
 * Sorcery
 *
 * Look at the top three cards of your library. Put one of them into your hand, put one of them on
 * the bottom of your library, and exile one of them. You may play the exiled card this turn.
 *
 * Two splits over the looked-at cards: first the card for your hand, then — from the other two —
 * the card for the bottom; whatever is left is exiled with a this-turn play permission. Both
 * choices are made before anything moves. With fewer than three cards the instructions run in
 * order for the cards that are there (hand first, then bottom), so nothing is exiled from a
 * two-card library (2026-03-20 ruling).
 */
val ExpressiveIteration = card("Expressive Iteration") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Look at the top three cards of your library. Put one of them into your hand, put one of " +
        "them on the bottom of your library, and exile one of them. You may play the exiled card this turn."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(3))
            val (toHandCard, rest) = chooseExactlySplit(
                count = 1,
                from = looked,
                prompt = "Choose a card to put into your hand",
                selectedLabel = "Put into your hand",
                remainderLabel = "Not chosen",
            )
            val (toBottomCard, toExileCard) = chooseExactlySplit(
                count = 1,
                from = rest,
                prompt = "Choose a card to put on the bottom of your library (the other is exiled)",
                selectedLabel = "Put on the bottom",
                remainderLabel = "Exile (you may play it this turn)",
            )
            toHand(toHandCard)
            toLibraryBottom(toBottomCard)
            exile(toExileCard)
            run(Effects.GrantMayPlayFromExile(toExileCard))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Anastasia Ovchinnikova"
        flavorText = "Prismari students dance in the tension between passion and perfection."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31b770cc-09e7-4c0b-b2a4-462ab4f7200d.jpg?1783927314"

        ruling("2026-03-20", "If you choose to play the exiled card this turn, you must still pay all costs and follow all timing restrictions required by that card. If it's a land, you can't play it unless you have a land play available.")
        ruling("2026-03-20", "If there are fewer than three cards in your library, follow the instructions in the order given for any cards that are there. For example, if there are two cards in your library, you'll put one of them into your hand and one of them on the bottom of your library. You won't have the option to exile either of them.")
        ruling("2026-03-20", "If you don't play the exiled card, it remains exiled. It won't be available to be played on future turns.")
    }
}
