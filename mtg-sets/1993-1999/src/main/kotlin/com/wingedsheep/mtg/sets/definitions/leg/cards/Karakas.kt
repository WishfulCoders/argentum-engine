package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Karakas
 * Legendary Land
 * {T}: Add {W}.
 * {T}: Return target legendary creature to its owner's hand.
 */
val Karakas = card("Karakas") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Legendary Land"
    oracleText = "{T}: Add {W}.\n{T}: Return target legendary creature to its owner's hand."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature.legendary())
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "303"
        artist = "Nicola Leonard"
        flavorText = "\"To make a prairie it takes a clover and one bee,/ One clover, and a bee,/ And revery.\" —Emily Dickinson"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31d2422a-bb7d-4cdd-9aac-e5a936a4be3b.jpg?1783948023"
    }
}
