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
 * Dismal Backwater
 * Land
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {U} or {B}.
 */
val DismalBackwater = card("Dismal Backwater") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {U} or {B}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

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

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "232"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63742780-47ee-4a66-993a-69e06c14967d.jpg?1562787616"
    }
}
