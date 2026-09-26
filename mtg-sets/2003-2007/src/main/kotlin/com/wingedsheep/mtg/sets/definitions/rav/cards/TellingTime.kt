package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Telling Time — Ravnica: City of Guilds #69
 * {1}{U} · Instant
 *
 * Look at the top three cards of your library. Put one of those cards into your hand, one on top
 * of your library, and one on the bottom of your library.
 *
 * Modelling notes:
 * - Three destinations, so this is one gather feeding *two* selections rather than the single
 *   keep/rest split [com.wingedsheep.sdk.dsl.Patterns.Library.lookAtTopAndKeep] models: pick the
 *   card for your hand, then pick which of the two survivors goes back on top. The remainder of
 *   the second selection is the bottom card, so no third decision is needed — with two cards
 *   left, naming one names the other.
 * - Every card the gather looked at is moved back explicitly. Nothing is left in the collection,
 *   which is what keeps a three-card library (or a smaller one) from stranding cards.
 * - A library with fewer than three cards simply looks at what is there: `ChooseExactly(1)` on an
 *   empty or one-card collection resolves to what is available, and the later moves are no-ops.
 */
val TellingTime = card("Telling Time") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at the top three cards of your library. Put one of those cards into your " +
        "hand, one on top of your library, and one on the bottom of your library."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(3))
            val (toHandCards, notTaken) = chooseExactlySplit(
                1,
                from = looked,
                selectedLabel = "Put into your hand",
                remainderLabel = "Keep in your library"
            )
            toHand(toHandCards)
            val (toTop, toBottom) = chooseExactlySplit(
                1,
                from = notTaken,
                selectedLabel = "Put on top of your library",
                remainderLabel = "Put on the bottom of your library"
            )
            toLibraryTop(toTop, order = CardOrder.Preserve)
            toLibraryBottom(toBottom, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Scott M. Fischer"
        flavorText = "Mastery is achieved when \"telling time\" becomes \"telling time what to do.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/563e6cf4-e86f-4538-85a1-3abbc83b303d.jpg?1783943677"
    }
}
