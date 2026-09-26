package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Keeper of the Nine Gales
 * {2}{U}
 * Creature — Bird Wizard
 * 1/2
 * Flying
 * {T}, Tap two untapped Birds you control: Return target permanent to its owner's hand.
 */
val KeeperOfTheNineGales = card("Keeper of the Nine Gales") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Wizard"
    power = 1
    toughness = 2
    oracleText = "Flying\n{T}, Tap two untapped Birds you control: Return target permanent to its owner's hand."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(2, GameObjectFilter.Permanent.withSubtype("Bird"))
        )
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.ReturnToHand(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Jim Nelson"
        flavorText = "\"You cannot fight the storm.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f75eef50-b474-44bb-8222-3e473928304a.jpg?1562944856"
    }
}
