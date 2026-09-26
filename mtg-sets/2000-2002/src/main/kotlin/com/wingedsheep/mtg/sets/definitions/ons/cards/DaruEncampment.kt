package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Daru Encampment
 * Land
 * {T}: Add {C}.
 * {W}, {T}: Target Soldier creature gets +1/+1 until end of turn.
 */
val DaruEncampment = card("Daru Encampment") {
    typeLine = "Land"
    colorIdentity = "W"
    oracleText = "{T}: Add {C}.\n{W}, {T}: Target Soldier creature gets +1/+1 until end of turn."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val t = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Soldier")))
        effect = Effects.ModifyStats(1, 1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "315"
        artist = "Tony Szczudlo"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5869f08-fac8-44b6-8142-7d7ecccab414.jpg?1562941659"
    }
}
