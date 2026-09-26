package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AddSubtypeEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Navigator's Compass
 * {1}
 * Artifact
 *
 * When this artifact enters, you gain 3 life.
 * {T}: Until end of turn, target land you control becomes the basic land type
 *      of your choice in addition to its other types.
 */
val NavigatorsCompass = card("Navigator's Compass") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, you gain 3 life.\n" +
        "{T}: Until end of turn, target land you control becomes the basic land type of your choice in addition to its other types."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
    }

    activatedAbility {
        val land = target(TargetFilter.Land.youControl())
        cost = AbilityCost.Tap
        effect = Effects.Pipeline {
            val chosenLandType = chooseOption(OptionType.BASIC_LAND_TYPE)
            run(Effects.AddSubtype(target = land, fromChosen = chosenLandType, duration = Duration.EndOfTurn))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "225"
        artist = "Sung Choi"
        flavorText = "The Weatherlight can no longer planeshift, but it can traverse Dominaria with ease."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a283135-7a51-4cf7-82a6-7e50894e64a5.jpg?1562737167"
        ruling("2018-04-27", "Gaining a basic land type causes the target land to gain the corresponding mana ability. Because the new basic land type is \"in addition to\" its other types, it keeps the abilities it had previously.")
    }
}
