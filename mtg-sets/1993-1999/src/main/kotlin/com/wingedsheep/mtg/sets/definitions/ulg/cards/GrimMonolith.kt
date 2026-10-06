package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grim Monolith
 * {2}
 * Artifact
 *
 * This artifact doesn't untap during your untap step.
 * {T}: Add {C}{C}{C}.
 * {4}: Untap this artifact.
 */
val GrimMonolith = card("Grim Monolith") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact doesn't untap during your untap step.\n" +
        "{T}: Add {C}{C}{C}.\n" +
        "{4}: Untap this artifact."

    flags(AbilityFlag.DOESNT_UNTAP)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{4}")
        effect = Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Chippy"
        flavorText = "Part prison, part home."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9ddc9fe1-17c8-4e1d-aeb8-c4214e881280.jpg?1783946223"
    }
}
