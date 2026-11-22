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
 * Tainted Field
 * Land
 *
 * {T}: Add {C}.
 * {T}: Add {W} or {B}. Activate only if you control a Swamp.
 *
 * "Add {W} or {B}" is spelled as two mana abilities (the Tainted Wood shape), each gated by
 * the same "control a Swamp" restriction — any permanent with the Swamp subtype counts.
 */
val TaintedField = card("Tainted Field") {
    typeLine = "Land"
    colorIdentity = "WB"
    oracleText = "{T}: Add {C}.\n{T}: Add {W} or {B}. Activate only if you control a Swamp."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlPermanentOfType(Subtype.SWAMP)))
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlPermanentOfType(Subtype.SWAMP)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Don Hazeltine"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75b5f0aa-1570-4064-ad20-aac7be8b2c9c.jpg?1783945139"
    }
}
