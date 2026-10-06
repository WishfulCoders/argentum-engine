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
 * Talisman of Resilience
 * {2}
 * Artifact
 * {T}: Add {C}.
 * {T}: Add {B} or {G}. This artifact deals 1 damage to you.
 *
 * The Modern Horizons Talisman, built like its Mirrodin cycle: the "{B} or {G}" line is two mana
 * abilities, each adding its color and then having the Talisman deal 1 damage to its controller.
 */
val TalismanOfResilience = card("Talisman of Resilience") {
    manaCost = "{2}"
    colorIdentity = "BG"
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n{T}: Add {B} or {G}. This artifact deals 1 damage to you."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK) then Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN) then Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Lindsey Look"
        flavorText = "\"The continual rise of the downtrodden is as inevitable as vines usurping a forest.\"\n—Vraska"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc25a254-edd2-4817-ab80-7373239da7d2.jpg?1783933070"
    }
}
