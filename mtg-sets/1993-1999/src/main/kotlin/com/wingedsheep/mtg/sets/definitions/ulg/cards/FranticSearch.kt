package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Frantic Search
 * {2}{U}
 * Instant
 *
 * Draw two cards, then discard two cards. Untap up to three lands.
 *
 * "Untap up to three lands" is not targeted and is not limited to lands you control (2022-12-08
 * ruling), so the lands are gathered from the whole battlefield ([CardSource.BattlefieldMatching])
 * and chosen as the spell resolves with a `chooseUpTo(3)`, then untapped with a non-tapping
 * [Effects.TapCollection].
 */
val FranticSearch = card("Frantic Search") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw two cards, then discard two cards. Untap up to three lands."

    spell {
        effect = Effects.DrawCards(2) then Effects.Discard(2) then
            Effects.Pipeline {
                val lands = gather(CardSource.BattlefieldMatching(GameObjectFilter.Land))
                val toUntap = chooseUpTo(3, from = lands)
                run(Effects.TapCollection(collection = toUntap, tap = false))
            }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Jeff Miracola"
        flavorText = "Motivation was high in the academy once students realized flunking their exams could kill them."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/1904db14-6df7-424f-afa5-e3dfab31300a.jpg?1783946247"
        ruling(
            "2022-12-08",
            "You choose which lands to untap as the spell resolves. They aren't targeted, and they don't " +
                "have to be lands that you control."
        )
        ruling(
            "2018-12-07",
            "You draw two cards and discard two cards all while Frantic Search is resolving. Nothing can " +
                "happen between the two, and no player may choose to take actions."
        )
    }
}
