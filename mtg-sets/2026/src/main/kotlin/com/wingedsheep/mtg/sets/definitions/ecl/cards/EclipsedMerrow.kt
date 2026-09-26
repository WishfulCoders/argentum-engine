package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.dsl.Effects

/**
 * Eclipsed Merrow
 * {W/U}{W/U}{W/U}
 * Creature — Merfolk Scout
 * 2/3
 *
 * When this creature enters, look at the top four cards of your library.
 * You may reveal a Merfolk, Plains, or Island card from among them and
 * put it into your hand. Put the rest on the bottom of your library in
 * a random order.
 */
val EclipsedMerrow = card("Eclipsed Merrow") {
    manaCost = "{W/U}{W/U}{W/U}"
    colorIdentity = "WU"
    typeLine = "Creature — Merfolk Scout"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal a Merfolk, Plains, or Island card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(listOf(
                        CardPredicate.HasSubtype(Subtype.MERFOLK),
                        CardPredicate.HasSubtype(Subtype.PLAINS),
                        CardPredicate.HasSubtype(Subtype.ISLAND),
                        ))
                    )
                ),
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2352750d-404d-4928-9bdb-1b0db599b70f.jpg?1767952376"
    }
}
