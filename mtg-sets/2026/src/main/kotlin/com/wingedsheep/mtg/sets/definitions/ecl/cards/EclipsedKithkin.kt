package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.dsl.Effects

/**
 * Eclipsed Kithkin
 * {G/W}{G/W}
 * Creature — Kithkin Scout
 * 2/1
 *
 * When this creature enters, look at the top four cards of your library.
 * You may reveal a Kithkin, Forest, or Plains card from among them and
 * put it into your hand. Put the rest on the bottom of your library in
 * a random order.
 */
val EclipsedKithkin = card("Eclipsed Kithkin") {
    manaCost = "{G/W}{G/W}"
    colorIdentity = "WG"
    typeLine = "Creature — Kithkin Scout"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal a Kithkin, Forest, or Plains card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

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
                        CardPredicate.HasSubtype(Subtype.KITHKIN),
                        CardPredicate.HasSubtype(Subtype.FOREST),
                        CardPredicate.HasSubtype(Subtype.PLAINS),
                        ))
                    )
                ),
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            move(kept, CardDestination.ToZone(Zone.HAND), revealed = true, revealToSelf = false)
            toLibraryBottom(rest, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29e1cfa4-0ad8-4228-9f7e-cbab114d1d5f.jpg?1767952368"
    }
}
