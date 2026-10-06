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
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Endurance — Modern Horizons 2 #157
 * {1}{G}{G} · Creature — Elemental Incarnation · 3 / 4
 *
 * Flash
 * Reach
 * When this creature enters, up to one target player puts all the cards from their graveyard on
 * the bottom of their library in a random order.
 * Evoke—Exile a green card from your hand.
 *
 * The evoke cost is non-mana only ([evokeWith] an exile-a-green-card-from-hand cost); the evoke
 * sacrifice trigger is the engine's. The enters trigger gathers the target player's whole
 * graveyard and moves it to the bottom of that player's library in [CardOrder.Random].
 */
val Endurance = card("Endurance") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental Incarnation"
    power = 3
    toughness = 4
    oracleText = "Flash\n" +
        "Reach\n" +
        "When this creature enters, up to one target player puts all the cards from their graveyard " +
        "on the bottom of their library in a random order.\n" +
        "Evoke—Exile a green card from your hand."

    keywords(Keyword.FLASH, Keyword.REACH)

    evokeWith(
        Costs.additional.ExileCards(
            count = 1,
            filter = GameObjectFilter.Any.withColor(Color.GREEN),
            fromZone = CostZone.HAND
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player, optional = true)
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
            move(
                graveyard,
                CardDestination.ToZone(Zone.LIBRARY, player.asPlayer, ZonePlacement.Bottom),
                order = CardOrder.Random
            )
        }
        description = "up to one target player puts all the cards from their graveyard on the bottom " +
            "of their library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "157"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb0e0404-4846-4891-acfa-bd0951ecf9c6.jpg?1783926831"
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
