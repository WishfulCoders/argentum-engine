package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Contact Other Plane
 * {3}{U}
 * Instant
 * Roll a d20.
 * 1—9 | Draw two cards.
 * 10—19 | Scry 2, then draw two cards.
 * 20 | Scry 3, then draw three cards.
 *
 * The d20 results table (CR 706.3) is [Patterns.Mechanic.rollDie]. The scry rows pause for the
 * top/bottom decision and resume into their own draw; the other rows stay off.
 */
val ContactOtherPlane = card("Contact Other Plane") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Roll a d20.\n1—9 | Draw two cards.\n10—19 | Scry 2, then draw two cards.\n" +
        "20 | Scry 3, then draw three cards."

    spell {
        effect = Patterns.Mechanic.rollDie(
            20,
            1..9 to Effects.DrawCards(2),
            10..19 to (Effects.Scry(2) then Effects.DrawCards(2)),
            20..20 to (Effects.Scry(3) then Effects.DrawCards(3)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6fefb38-c6f2-43c4-a6b9-ac82f8827bc2.jpg?1783926518"
    }
}
