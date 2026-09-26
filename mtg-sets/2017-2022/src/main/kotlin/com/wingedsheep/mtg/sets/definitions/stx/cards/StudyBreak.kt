package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Study Break — Strixhaven: School of Mages #34 (canonical printing)
 * {1}{W} · Instant
 *
 * Tap up to two target creatures.
 * Learn.
 *
 * "Up to two" is [Targets.UpToCreatures] (`optional = true`), so zero and one are both legal
 * choices and the spell still resolves — the Learn is the floor that keeps it from being a dead
 * card with no creatures on the board. [Effects.TapEachTarget] taps however many were actually
 * chosen rather than assuming two.
 *
 * `Learn` is [Patterns.Mechanic.learn] (CR 701.48).
 */
val StudyBreak = card("Study Break") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Tap up to two target creatures.\n" +
        "Learn. (You may reveal a Lesson card you own from outside the game and put it into your " +
        "hand, or discard a card to draw a card.)"

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.TapEachTarget() then Patterns.Mechanic.learn()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Cristi Balanescu"
        flavorText = "\"You've been cramming all night. You're taking a nap whether you like it or not.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef7cd813-0781-4fd7-8748-2716e1eeb4b9.jpg?1783927382"
    }
}
