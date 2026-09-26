package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Thornwood Falls
 * Land
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {G} or {U}.
 */
val ThornwoodFalls = card("Thornwood Falls") {
    typeLine = "Land"
    colorIdentity = "UG"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {G} or {U}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "244"
        artist = "Eytan Zana"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e57abd9-e864-4047-a3c8-618952071858.jpg?1562791123"
    }
}
