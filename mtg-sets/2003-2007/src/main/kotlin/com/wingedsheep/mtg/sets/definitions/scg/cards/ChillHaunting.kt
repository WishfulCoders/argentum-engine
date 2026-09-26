package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chill Haunting
 * {1}{B}
 * Instant
 * As an additional cost to cast this spell, exile X creature cards from your graveyard.
 * Target creature gets -X/-X until end of turn.
 */
val ChillHaunting = card("Chill Haunting") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, exile X creature cards from your graveyard.\nTarget creature gets -X/-X until end of turn."

    additionalCost(Costs.additional.ExileVariableCards(
        minCount = 1,
        filter = GameObjectFilter.Creature,
        fromZone = CostZone.GRAVEYARD
    ))

    spell {
        val creature = target(TargetFilter.Creature)
        val negX = -DynamicAmounts.additionalCostExiledCount()
        effect = Effects.ModifyStats(negX, negX, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91035d03-2bf8-4e6b-945b-4dfbed0873ec.jpg?1562532116"
    }
}
