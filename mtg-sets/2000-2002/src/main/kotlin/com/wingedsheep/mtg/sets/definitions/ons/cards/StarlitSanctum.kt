package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.dsl.Targets

/**
 * Starlit Sanctum
 * Land
 * {T}: Add {C}.
 * {W}, {T}, Sacrifice a Cleric creature: You gain life equal to the sacrificed creature's toughness.
 * {B}, {T}, Sacrifice a Cleric creature: Target player loses life equal to the sacrificed creature's power.
 */
val StarlitSanctum = card("Starlit Sanctum") {
    typeLine = "Land"
    colorIdentity = "WB"
    oracleText = "{T}: Add {C}.\n{W}, {T}, Sacrifice a Cleric creature: You gain life equal to the sacrificed creature's toughness.\n{B}, {T}, Sacrifice a Cleric creature: Target player loses life equal to the sacrificed creature's power."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Cleric"))
        )
        effect = Effects.GainLife(
            amount = DynamicAmounts.sacrificedToughness(),
            target = EffectTarget.Controller
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Cleric"))
        )
        val t = target(Targets.Player)
        effect = Effects.LoseLife(
            amount = DynamicAmounts.sacrificedPower(),
            target = t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "325"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ace5e601-2583-4d9c-8bdf-aa33666c717c.jpg?1562935857"
    }
}
