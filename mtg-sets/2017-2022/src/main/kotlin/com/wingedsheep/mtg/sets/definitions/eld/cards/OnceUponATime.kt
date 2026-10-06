package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Once Upon a Time — Throne of Eldraine #169
 * {1}{G} · Instant
 *
 * If this spell is the first spell you've cast this game, you may cast it without paying its mana
 * cost.
 * Look at the top five cards of your library. You may reveal a creature or land card from among them
 * and put it into your hand. Put the rest on the bottom of your library in a random order.
 *
 * "Without paying its mana cost" is an alternative cost (CR 118.9), offered only while the game-long
 * spell count ([DynamicAmounts.spellsCastThisGame]) is still 0 — read as the spell is proposed, so
 * it never counts itself (CR 601.2i records the cast only once casting completes). The mana value
 * stays 2 (CR 118.9c). Earliest window: the first player's upkeep (ruling).
 */
val OnceUponATime = card("Once Upon a Time") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "If this spell is the first spell you've cast this game, you may cast it without paying " +
        "its mana cost.\n" +
        "Look at the top five cards of your library. You may reveal a creature or land card from among " +
        "them and put it into your hand. Put the rest on the bottom of your library in a random order."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        condition = Conditions.CompareAmounts(DynamicAmounts.spellsCastThisGame(), ComparisonOperator.EQ, 0),
    )

    spell {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Creature or GameObjectFilter.Land,
            prompt = "You may reveal a creature or land card and put it into your hand",
            restOrder = CardOrder.Random,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/4034e5ba-9974-43e3-bde7-8d9b4586c3a4.jpg?1783932607"
        ruling("2019-10-04", "The earliest opportunity you have to cast Once Upon a Time is during the first player's upkeep, before that player can play a land.")
    }
}
