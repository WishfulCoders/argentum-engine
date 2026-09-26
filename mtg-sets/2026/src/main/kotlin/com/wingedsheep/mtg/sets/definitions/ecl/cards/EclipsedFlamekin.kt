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
 * Eclipsed Flamekin
 * {1}{U/R}{U/R}
 * Creature — Elemental Scout
 * 1/4
 *
 * When this creature enters, look at the top four cards of your library.
 * You may reveal an Elemental, Island, or Mountain card from among them
 * and put it into your hand. Put the rest on the bottom of your library
 * in a random order.
 */
val EclipsedFlamekin = card("Eclipsed Flamekin") {
    manaCost = "{1}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Creature — Elemental Scout"
    power = 1
    toughness = 4
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal an Elemental, Island, or Mountain card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

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
                        CardPredicate.HasSubtype(Subtype.ELEMENTAL),
                        CardPredicate.HasSubtype(Subtype.ISLAND),
                        CardPredicate.HasSubtype(Subtype.MOUNTAIN),
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
        collectorNumber = "219"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d907ae44-cd07-4409-946c-e97f584d9a81.jpg?1767749662"
    }
}
