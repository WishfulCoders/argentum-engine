package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Celestial Colonnade
 * Land
 *
 * This land enters tapped.
 * {T}: Add {W} or {U}.
 * {3}{W}{U}: Until end of turn, this land becomes a 4/4 white and blue Elemental creature with
 * flying and vigilance. It's still a land.
 */
val CelestialColonnade = card("Celestial Colonnade") {
    typeLine = "Land"
    colorIdentity = "WU"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {W} or {U}.\n" +
        "{3}{W}{U}: Until end of turn, this land becomes a 4/4 white and blue Elemental creature " +
        "with flying and vigilance. It's still a land."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}{U}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            creatureTypes = setOf("Elemental"),
            colors = setOf(Color.WHITE.name, Color.BLUE.name),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6929259-2903-4f6f-9b06-42048fd55c6a.jpg?1783942037"

        ruling("2018-12-07", "Once Celestial Colonnade has attacked, tapping it for mana won't remove it from combat.")
        ruling("2010-03-01", "A land that becomes a creature may be affected by \"summoning sickness.\" You can't attack with it or use any of its {T} abilities (including its mana abilities) unless it began your most recent turn on the battlefield under your control. Note that summoning sickness cares about when that permanent came under your control, not when it became a creature.")
        ruling("2010-03-01", "When a land becomes a creature, that doesn't count as having a creature enter. The permanent was already on the battlefield; it only changed its types. Abilities that trigger whenever a creature enters won't trigger.")
    }
}
