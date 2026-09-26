package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Duelist of the Mind
 * {1}{U}
 * Creature — Human Advisor
 * Power/toughness: star/3
 * Flying, vigilance
 * Duelist of the Mind's power is equal to the number of cards you've drawn this turn.
 * Whenever you commit a crime, you may draw a card. If you do, discard a card. This ability
 * triggers only once each turn.
 *
 * The characteristic-defining power uses [DynamicAmount.TurnTracking] with the new
 * [TurnTracker.CARDS_DRAWN] tracker (backed by `CardsDrawnThisTurnComponent`); only power is
 * dynamic, toughness stays a printed 3, so we use the single-stat `dynamicPower(...)` helper
 * rather than the both-stats `dynamicStats(...)`.
 *
 * The crime trigger is the standard `Triggers.you.commitsCrime()` capped with `oncePerTurn = true`
 * and runs the optional loot ([Effects.May] wrapping [Patterns.Hand.loot]) — the same composition
 * as Jeskai Elder.
 */
val DuelistOfTheMind = card("Duelist of the Mind") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Advisor"
    oracleText = "Flying, vigilance\nDuelist of the Mind's power is equal to the number of cards you've drawn this turn.\nWhenever you commit a crime, you may draw a card. If you do, discard a card. This ability triggers only once each turn."
    toughness = 3
    dynamicPower(
        DynamicAmounts.cardsDrawnThisTurn(Player.You)
    )

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.commitsCrime()
        oncePerTurn = true
        effect = Effects.May(Patterns.Hand.loot())
        description = "Whenever you commit a crime, you may draw a card. If you do, discard a card. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Darren Tan"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b58e47b-c165-4a58-aa2a-033a35645adc.jpg?1716283553"
    }
}
