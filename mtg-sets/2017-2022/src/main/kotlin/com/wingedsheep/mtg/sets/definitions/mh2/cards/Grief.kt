package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evokeWith
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Grief — Modern Horizons 2 #87
 * {2}{B}{B} · Creature — Elemental Incarnation · 3 / 2
 *
 * Menace
 * When this creature enters, target opponent reveals their hand. You choose a nonland card from
 * it. That player discards that card.
 * Evoke—Exile a black card from your hand.
 *
 * The evoke cost is non-mana only ([evokeWith] an exile-a-black-card-from-hand cost); the evoke
 * sacrifice trigger is the engine's. The enters trigger is Thoughtseize's reveal-choose-discard
 * pipeline aimed at an opponent, without the life loss.
 */
val Grief = card("Grief") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elemental Incarnation"
    power = 3
    toughness = 2
    oracleText = "Menace\n" +
        "When this creature enters, target opponent reveals their hand. You choose a nonland card " +
        "from it. That player discards that card.\n" +
        "Evoke—Exile a black card from your hand."

    keywords(Keyword.MENACE)

    evokeWith(
        Costs.additional.ExileCards(
            count = 1,
            filter = GameObjectFilter.Any.withColor(Color.BLACK),
            fromZone = CostZone.HAND
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val revealedHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = revealedHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
        }
        description = "target opponent reveals their hand. You choose a nonland card from it. That " +
            "player discards that card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "87"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6befbc4-1320-4f26-bd9f-b1814fedda10.jpg?1783926861"
        ruling(
            "2021-06-18",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying (such as an evoke cost), add any cost increases, then apply any cost reductions. " +
                "The mana value of the spell is determined by only its mana cost, no matter what the total " +
                "cost to cast that spell was."
        )
        ruling(
            "2021-06-18",
            "If you pay the evoke cost, you can have the creature's own triggered ability resolve before " +
                "the evoke triggered ability. You can cast spells after that ability resolves but before " +
                "you have to sacrifice the creature."
        )
    }
}
