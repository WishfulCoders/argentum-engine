package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Infernal Idol
 * {3}
 * Artifact
 *
 * {T}: Add {B}.
 * {1}{B}{B}, {T}, Sacrifice this artifact: You draw two cards and lose 2 life.
 */
val InfernalIdol = card("Infernal Idol") {
    manaCost = "{3}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "{T}: Add {B}.\n" +
        "{1}{B}{B}, {T}, Sacrifice this artifact: You draw two cards and lose 2 life."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}{B}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Drew Tucker"
        flavorText = "\"The courier's death was regrettable, but then again, she was warned not to touch it.\"\n—Lord Xander"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d986229-c8d5-462c-bf74-ae94326a5aa0.jpg?1783919175"
    }
}
