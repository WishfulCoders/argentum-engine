package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Conch Horn
 * {2}
 * Artifact
 * {1}, {T}, Sacrifice this artifact: Draw two cards, then put a card from your hand on top of your
 * library.
 *
 * The put-back is from the *whole* hand, not just the two cards drawn, and it is mandatory —
 * so a player who draws into an empty hand still returns one of the two.
 */
val ConchHorn = card("Conch Horn") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Draw two cards, then put a card from your hand on top of your library."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            run(Effects.DrawCards(2))
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
            val toTop = chooseExactly(1, from = hand, selectedLabel = "Put on top of your library")
            toLibraryTop(toTop, order = CardOrder.Preserve)
        }
        description = "{1}, {T}, Sacrifice this artifact: Draw two cards, then put a card from your hand on top of your library."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Phil Foglio"
        flavorText = "Even the most skilled of modern mages only partially understand the Conch Horn's awesome powers."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/860a9ba3-e4c4-4af9-bdfe-1ada39289fd5.jpg?1783947881"
    }
}
