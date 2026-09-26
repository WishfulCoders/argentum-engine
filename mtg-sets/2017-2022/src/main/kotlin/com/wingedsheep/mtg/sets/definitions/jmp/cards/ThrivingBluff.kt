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
 * Thriving Bluff
 * Land
 * This land enters tapped. As it enters, choose a color other than red.
 * {T}: Add {R} or one mana of the chosen color.
 *
 * The color is chosen as the land enters (a replacement, CR 614.1c) from the four colors other
 * than red and stored on the permanent. The mana ability picks from the union of {R} and that
 * stored color, so a copy that entered without a choice still taps for {R}.
 */
val ThrivingBluff = card("Thriving Bluff") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Land"
    oracleText = "This land enters tapped. As it enters, choose a color other than red.\n{T}: Add {R} or one mana of the chosen color."

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithChoice(ChoiceType.COLOR, excludedColors = setOf(Color.RED)))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Union(listOf(ManaColorSet.Specific(setOf(Color.RED)), ManaColorSet.SourceChosenColor))
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Johannes Voss"
        flavorText = "As eons passed, the rock wore away, revealing the rich colors at the mountain's heart."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15481459-3703-4185-ad27-105d95691e9d.jpg?1783930498"
    }
}
