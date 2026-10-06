package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
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
 * Creeping Tar Pit
 * Land
 *
 * This land enters tapped.
 * {T}: Add {U} or {B}.
 * {1}{U}{B}: Until end of turn, this land becomes a 3/2 blue and black Elemental creature. It's
 * still a land. It can't be blocked this turn.
 */
val CreepingTarPit = card("Creeping Tar Pit") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {U} or {B}.\n" +
        "{1}{U}{B}: Until end of turn, this land becomes a 3/2 blue and black Elemental creature. " +
        "It's still a land. It can't be blocked this turn."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{1}{U}{B}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 2,
            creatureTypes = setOf("Elemental"),
            colors = setOf(Color.BLUE.name, Color.BLACK.name),
            duration = Duration.EndOfTurn,
        ) then Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f427f0b-034c-4821-8758-e395c0042d8a.jpg?1783942038"

        ruling("2010-03-01", "A land that becomes a creature may be affected by \"summoning sickness.\" You can't attack with it or use any of its {T} abilities (including its mana abilities) unless it began your most recent turn on the battlefield under your control. Note that summoning sickness cares about when that permanent came under your control, not when it became a creature.")
        ruling("2010-03-01", "When a land becomes a creature, that doesn't count as having a creature enter. The permanent was already on the battlefield; it only changed its types. Abilities that trigger whenever a creature enters won't trigger.")
    }
}
