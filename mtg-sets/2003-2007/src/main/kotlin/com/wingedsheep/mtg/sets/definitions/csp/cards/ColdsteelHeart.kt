package com.wingedsheep.mtg.sets.definitions.csp.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Coldsteel Heart
 * {2}
 * Snow Artifact
 *
 * This artifact enters tapped.
 * As this artifact enters, choose a color.
 * {T}: Add one mana of the chosen color.
 */
val ColdsteelHeart = card("Coldsteel Heart") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Snow Artifact"
    oracleText = "This artifact enters tapped.\nAs this artifact enters, choose a color.\n{T}: Add one mana of the chosen color."

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChosenColor()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Mark Romanoski"
        flavorText = "The Phyrexian death machine awoke, its coldsteel heart imbuing it with sinister new power."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdd5015e-fe31-42e3-923d-44b9cdece273.jpg?1783943319"
    }
}
