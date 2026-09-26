package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quag Feast
 * {1}{B}
 * Sorcery
 *
 * Choose target creature, planeswalker, or Vehicle. Mill two cards, then destroy the chosen
 * permanent if its mana value is less than or equal to the number of cards in your graveyard.
 *
 * Mill resolves first, then a [Effects.If] re-reads the (now larger) graveyard: a
 * [Compare] of the chosen target's mana value against the count of cards in your graveyard,
 * destroying it only when the threshold holds.
 */
val QuagFeast = card("Quag Feast") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose target creature, planeswalker, or Vehicle. Mill two cards, then destroy " +
        "the chosen permanent if its mana value is less than or equal to the number of cards in " +
        "your graveyard."

    spell {
        val creaturePlaneswalkerOrVehicle = target(TargetFilter(GameObjectFilter.CreatureOrVehicle or GameObjectFilter.Planeswalker))
        effect = Patterns.Library.mill(2) then Effects.If(
            condition = Conditions.CompareAmounts(
                left = DynamicAmounts.manaValueOf(creaturePlaneswalkerOrVehicle),
                operator = ComparisonOperator.LTE,
                right = DynamicAmounts.cardsInYourGraveyard(),
            ),
            then = Effects.Destroy(creaturePlaneswalkerOrVehicle),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "100"
        artist = "Loïc Canavaggia"
        flavorText = "Speedbrood racers are gregarious while not racing, but everyone must eat."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd8b6033-63e6-484e-8efb-a4eb9ca59fbf.jpg?1782687880"
    }
}
