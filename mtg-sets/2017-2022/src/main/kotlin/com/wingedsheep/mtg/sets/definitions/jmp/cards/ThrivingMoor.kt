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
 * Thriving Moor
 * Land
 * This land enters tapped. As it enters, choose a color other than black.
 * {T}: Add {B} or one mana of the chosen color.
 *
 * The color is chosen as the land enters (a replacement, CR 614.1c) from the four colors other
 * than black and stored on the permanent. The mana ability picks from the union of {B} and that
 * stored color, so a copy that entered without a choice still taps for {B}.
 */
val ThrivingMoor = card("Thriving Moor") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "This land enters tapped. As it enters, choose a color other than black.\n{T}: Add {B} or one mana of the chosen color."

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithChoice(ChoiceType.COLOR, excludedColors = setOf(Color.BLACK)))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Union(listOf(ManaColorSet.Specific(setOf(Color.BLACK)), ManaColorSet.SourceChosenColor))
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Titus Lunter"
        flavorText = "Beauty appears in strange places for those with eyes to see it."
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18756fe5-70f0-48d9-a4f1-ea78f77d2084.jpg?1783930496"
    }
}
