package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Territorial Kavu — Modern Horizons 2 #216
 * {R}{G} · Creature — Kavu · * / *
 *
 * Domain — Territorial Kavu's power and toughness are each equal to the number of basic land types
 * among lands you control.
 * Whenever this creature attacks, choose one —
 * • Discard a card. If you do, draw a card.
 * • Exile up to one target card from a graveyard.
 *
 * Domain is an ability word; the characteristic-defining P/T is [DynamicAmounts.domain] on both
 * stats (Nishoba Brawler's shape). The first mode is the rummage pattern — the draw counts the cards
 * actually discarded, so an empty hand draws nothing. The second mode's "up to one" target is an
 * optional graveyard target, any graveyard.
 */
val TerritorialKavu = card("Territorial Kavu") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Kavu"
    oracleText = "Domain — Territorial Kavu's power and toughness are each equal to the number of basic " +
        "land types among lands you control.\n" +
        "Whenever this creature attacks, choose one —\n" +
        "• Discard a card. If you do, draw a card.\n" +
        "• Exile up to one target card from a graveyard."

    dynamicStats(DynamicAmounts.domain())

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Modal(
            modes = listOf(
                Mode.noTarget(Patterns.Hand.rummage(1), "Discard a card. If you do, draw a card."),
                mode("Exile up to one target card from a graveyard") {
                    val card = target(TargetFilter.CardInGraveyard, optional = true)
                    effect = Effects.Exile(card)
                }
            ),
            chooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "216"
        artist = "E. M. Gist"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/2605df98-0b02-4aab-bc36-01e93c693743.jpg?1783926808"

        ruling(
            "2021-06-18",
            "Domain abilities count the number of basic land types among lands you control, not how many lands " +
                "you control or how many of any type."
        )
        ruling(
            "2021-06-18",
            "The basic land types are Plains, Island, Swamp, Mountain, and Forest. Land types other than basic " +
                "land types (such as Desert) don't contribute to domain abilities."
        )
    }
}
