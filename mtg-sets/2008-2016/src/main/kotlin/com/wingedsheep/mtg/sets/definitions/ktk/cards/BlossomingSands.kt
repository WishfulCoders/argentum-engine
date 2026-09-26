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
 * Blossoming Sands
 * Land
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {G} or {W}.
 */
val BlossomingSands = card("Blossoming Sands") {
    typeLine = "Land"
    colorIdentity = "WG"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {G} or {W}."

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
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "231"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a32a1c0b-f6ea-475a-aa01-3618ea7d8647.jpg?1562791365"
    }
}
