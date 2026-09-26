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
 * Thriving Isle
 * Land
 * This land enters tapped. As it enters, choose a color other than blue.
 * {T}: Add {U} or one mana of the chosen color.
 *
 * The color is chosen as the land enters (a replacement, CR 614.1c) from the four colors other
 * than blue and stored on the permanent. The mana ability picks from the union of {U} and that
 * stored color, so a copy that entered without a choice still taps for {U}.
 */
val ThrivingIsle = card("Thriving Isle") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Land"
    oracleText = "This land enters tapped. As it enters, choose a color other than blue.\n{T}: Add {U} or one mana of the chosen color."

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithChoice(ChoiceType.COLOR, excludedColors = setOf(Color.BLUE)))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Union(listOf(ManaColorSet.Specific(setOf(Color.BLUE)), ManaColorSet.SourceChosenColor))
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Jonas De Ro"
        flavorText = "All the wonders of the natural world in one breathtaking microcosm."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7eb8fd94-2b59-4b05-b4d0-c93497301d19.jpg?1783930498"
    }
}
