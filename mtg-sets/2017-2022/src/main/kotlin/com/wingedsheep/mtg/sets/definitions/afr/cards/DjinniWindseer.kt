package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Djinni Windseer
 * {3}{U}
 * Creature — Djinn
 * 3/3
 * Flying
 * When this creature enters, roll a d20.
 * 1—9 | Scry 1.
 * 10—19 | Scry 2.
 * 20 | Scry 3.
 *
 * The d20 results table (CR 706.3) is [Patterns.Mechanic.rollDie]: the roll happens as the trigger
 * resolves, and only the row holding the result runs, so exactly one scry of 1, 2 or 3 happens.
 */
val DjinniWindseer = card("Djinni Windseer") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn"
    oracleText = "Flying\nWhen this creature enters, roll a d20.\n1—9 | Scry 1.\n10—19 | Scry 2.\n20 | Scry 3."
    power = 3
    toughness = 3
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Mechanic.rollDie(
            20,
            1..9 to Effects.Scry(1),
            10..19 to Effects.Scry(2),
            20..20 to Effects.Scry(3),
        )
        description = "When this creature enters, roll a d20. 1—9 | Scry 1. 10—19 | Scry 2. 20 | Scry 3."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67a42698-bf3b-4352-8178-9c98abfc5b09.jpg?1783926516"
    }
}
