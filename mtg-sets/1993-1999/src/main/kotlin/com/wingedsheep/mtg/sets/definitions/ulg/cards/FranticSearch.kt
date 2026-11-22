package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Frantic Search — Urza's Legacy #32 (canonical printing)
 * {2}{U} · Instant
 *
 * Draw two cards, then discard two cards. Untap up to three lands.
 *
 * The untap is a choice made on resolution, not targets, and the lands needn't be yours (ruling):
 * gather every land on the battlefield, choose up to three, untap them. Draw and discard are one
 * uninterrupted sequence (Thoughtflare's shape).
 */
val FranticSearch = card("Frantic Search") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw two cards, then discard two cards. Untap up to three lands."

    spell {
        effect = Effects.DrawCards(2) then
            Patterns.Hand.discardCards(2) then
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
        ruling("2022-12-08", "You choose which lands to untap as the spell resolves. They aren't targeted, and they don't have to be lands that you control.")
        ruling("2018-12-07", "You draw two cards and discard two cards all while Frantic Search is resolving. Nothing can happen between the two, and no player may choose to take actions.")
    }
}
