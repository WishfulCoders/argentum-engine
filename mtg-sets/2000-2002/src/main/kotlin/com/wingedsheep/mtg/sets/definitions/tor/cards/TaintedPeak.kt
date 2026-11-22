package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Tainted Peak
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {B} or {R}. Activate only if you control a Swamp.
 *
 * "Add {B} or {R}" is spelled as two mana abilities (the Tainted Wood shape), each gated by
 * the same "control a Swamp" restriction — any permanent with the Swamp subtype counts.
 */
val TaintedPeak = card("Tainted Peak") {
    typeLine = "Land"
    colorIdentity = "BR"
    oracleText = "{T}: Add {C}.\n{T}: Add {B} or {R}. Activate only if you control a Swamp."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlPermanentOfType(Subtype.SWAMP)))
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlPermanentOfType(Subtype.SWAMP)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Tony Szczudlo"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dcaaabe-e1d7-4047-9960-79178af3d903.jpg?1783945139"
    }
}
