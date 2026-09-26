package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Illegitimate Business
 * Land
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {B} or {G}.
 */
val IllegitimateBusiness = card("Illegitimate Business") {
    typeLine = "Land"
    colorIdentity = "BG"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {B} or {G}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Miklós Ligeti"
        flavorText = "Their recipe isn't the only secret."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71597acf-1ce6-46a8-b6c0-88755c8a377c.jpg?1771587098"
    }
}
