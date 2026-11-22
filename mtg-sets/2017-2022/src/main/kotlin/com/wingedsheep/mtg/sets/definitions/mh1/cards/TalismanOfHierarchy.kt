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
 * Talisman of Hierarchy — Modern Horizons #233 (canonical printing)
 * {2} · Artifact
 *
 * {T}: Add {C}.
 * {T}: Add {W} or {B}. This artifact deals 1 damage to you.
 *
 * The Mirrodin Talisman shape: the coloured line is one mana ability per colour, each carrying the
 * 1 damage.
 */
val TalismanOfHierarchy = card("Talisman of Hierarchy") {
    manaCost = "{2}"
    colorIdentity = "WB"
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n{T}: Add {W} or {B}. This artifact deals 1 damage to you."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE) then Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK) then Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Lindsey Look"
        flavorText = "\"You'll never get to the top if you don't know who's already there.\"\n—Kaya"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/826f99c7-f534-4183-8f0d-efe1609808ac.jpg?1783933070"
    }
}
