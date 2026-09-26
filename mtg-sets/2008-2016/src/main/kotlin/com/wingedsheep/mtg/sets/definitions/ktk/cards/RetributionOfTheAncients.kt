package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Retribution of the Ancients
 * {B}
 * Enchantment
 * {B}, Remove X +1/+1 counters from among creatures you control: Target creature gets -X/-X until end of turn.
 */
val RetributionOfTheAncients = card("Retribution of the Ancients") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "{B}, Remove X +1/+1 counters from among creatures you control: Target creature gets -X/-X until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"),
            Costs.RemoveXCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                filter = GameObjectFilter.Creature
            ))
        val creature = target(TargetFilter.Creature)
        val negX = -DynamicAmounts.xValue()
        effect = Effects.ModifyStats(negX, negX, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Svetlin Velinov"
        flavorText = "Abzan ancestors died to protect their Houses, and they protect them still."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d3ee29d-b374-471d-80c3-bcad7a4226e6.jpg?1562782474"
    }
}
