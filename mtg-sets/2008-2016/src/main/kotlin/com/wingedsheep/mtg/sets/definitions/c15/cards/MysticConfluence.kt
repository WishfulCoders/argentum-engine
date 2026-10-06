package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mystic Confluence
 * {3}{U}{U}
 * Instant
 *
 * Choose three. You may choose the same mode more than once.
 * • Counter target spell unless its controller pays {3}.
 * • Return target creature to its owner's hand.
 * • Draw a card.
 *
 * [modal] with `chooseCount = 3, allowRepeat = true`; each chosen instance of a targeted mode
 * picks its own target (the same one or a different one), and the modes resolve in printed order
 * (2021-03-19 ruling). Choosing the counter mode twice on one spell asks for {3} twice.
 */
val MysticConfluence = card("Mystic Confluence") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose three. You may choose the same mode more than once.\n" +
        "• Counter target spell unless its controller pays {3}.\n" +
        "• Return target creature to its owner's hand.\n" +
        "• Draw a card."

    spell {
        modal(chooseCount = 3, allowRepeat = true) {
            mode("Counter target spell unless its controller pays {3}") {
                target(TargetFilter.SpellOnStack)
                effect = Effects.CounterUnlessPays("{3}")
            }
            mode("Return target creature to its owner's hand") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Move(creature, Zone.HAND)
            }
            mode("Draw a card", Effects.DrawCards(1))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81bbffc2-6f58-4baa-8f95-168eab106b15.jpg?1783938114"
        ruling(
            "2021-03-19",
            "No matter which combination of modes you choose, you always follow the instructions in the " +
                "order they are written."
        )
        ruling(
            "2021-03-19",
            "If the middle mode is chosen more than once, you choose the relative order to return the " +
                "target creatures."
        )
        ruling(
            "2021-03-19",
            "If you choose the first and/or second modes but all of the targets become illegal before Mystic " +
                "Confluence resolves, the spell won't resolve. If you also chose the last mode once or twice, " +
                "you won't draw any cards. If at least one target is still legal, the spell will resolve but " +
                "will have no effect on any illegal targets."
        )
    }
}
