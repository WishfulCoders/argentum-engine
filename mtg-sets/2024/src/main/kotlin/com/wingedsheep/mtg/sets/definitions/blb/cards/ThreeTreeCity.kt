package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Three Tree City
 * Legendary Land
 *
 * As Three Tree City enters, choose a creature type.
 * {T}: Add {C}.
 * {2}, {T}: Choose a color. Add an amount of mana of that color equal to
 * the number of creatures you control of the chosen type.
 */
val ThreeTreeCity = card("Three Tree City") {
    typeLine = "Legendary Land"
    colorIdentity = ""
    oracleText = "As Three Tree City enters, choose a creature type.\n{T}: Add {C}.\n{2}, {T}: Choose a color. Add an amount of mana of that color equal to the number of creatures you control of the chosen type."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.AddAnyColorMana(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withChosenSubtype()
            ).count()
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "260"
        artist = "Grady Frederick"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56f88a48-cced-4a9d-8c19-e4f105f0d8a2.jpg?1721427358"
    }
}
