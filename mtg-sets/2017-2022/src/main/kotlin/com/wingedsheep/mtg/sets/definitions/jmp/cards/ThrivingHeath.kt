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
 * Thriving Heath
 * Land
 * This land enters tapped. As it enters, choose a color other than white.
 * {T}: Add {W} or one mana of the chosen color.
 *
 * The color is chosen as the land enters (a replacement, CR 614.1c) from the four colors other
 * than white and stored on the permanent. The mana ability picks from the union of {W} and that
 * stored color, so a copy that entered without a choice still taps for {W}.
 */
val ThrivingHeath = card("Thriving Heath") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Land"
    oracleText = "This land enters tapped. As it enters, choose a color other than white.\n{T}: Add {W} or one mana of the chosen color."

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithChoice(ChoiceType.COLOR, excludedColors = setOf(Color.WHITE)))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Union(listOf(ManaColorSet.Specific(setOf(Color.WHITE)), ManaColorSet.SourceChosenColor))
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Alayna Danner"
        flavorText = "Wildflowers here bloom not with the season but with the ebb and flow of magic."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe424eb3-7df8-4317-8776-6d960afbb90a.jpg?1783930498"
    }
}
