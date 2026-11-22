package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Talisman of Creativity — Modern Horizons #231 (canonical printing)
 * {2} · Artifact
 *
 * {T}: Add {C}.
 * {T}: Add {U} or {R}. This artifact deals 1 damage to you.
 *
 * The Mirrodin Talisman shape: the coloured line is one mana ability per colour, each carrying the
 * 1 damage.
 */
val TalismanOfCreativity = card("Talisman of Creativity") {
    manaCost = "{2}"
    colorIdentity = "UR"
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n{T}: Add {U} or {R}. This artifact deals 1 damage to you."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE) then Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED) then Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Lindsey Look"
        flavorText = "\"Good ideas don't take time. They take a lot of bad ideas first.\"\n—Ral Zarek"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d9dbadd-c1b6-44fe-92ac-6f69d7178342.jpg?1783933072"
    }
}
