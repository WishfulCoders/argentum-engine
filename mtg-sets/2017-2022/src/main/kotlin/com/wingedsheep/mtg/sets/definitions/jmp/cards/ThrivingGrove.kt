package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Thriving Grove
 * Land
 * This land enters tapped. As it enters, choose a color other than green.
 * {T}: Add {G} or one mana of the chosen color.
 *
 * The color is chosen as the land enters (a replacement, CR 614.1c) from the four colors other
 * than green and stored on the permanent. The mana ability picks from the union of {G} and that
 * stored color, so a copy that entered without a choice still taps for {G}.
 */
val ThrivingGrove = card("Thriving Grove") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Land"
    oracleText = "This land enters tapped. As it enters, choose a color other than green.\n{T}: Add {G} or one mana of the chosen color."

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithChoice(ChoiceType.COLOR, excludedColors = setOf(Color.GREEN)))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Union(listOf(ManaColorSet.Specific(setOf(Color.GREEN)), ManaColorSet.SourceChosenColor))
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Ravenna Tran"
        flavorText = "Far from human eyes, birds flit between vivid blossoms in a hidden paradise."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79cabbe0-1c44-4888-8ebc-25a4c3e2c5d7.jpg?1783930498"
    }
}
