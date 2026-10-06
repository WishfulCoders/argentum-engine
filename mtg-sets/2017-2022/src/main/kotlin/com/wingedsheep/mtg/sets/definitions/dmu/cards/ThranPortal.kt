package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantChosenSubtype
import com.wingedsheep.sdk.scripting.ManaAbilitiesCostAdditionalLife

/**
 * Thran Portal
 * Land — Gate
 *
 * This land enters tapped unless you control two or fewer other lands.
 * As this land enters, choose a basic land type.
 * This land is the chosen type in addition to its other types.
 * Mana abilities of this land cost an additional 1 life to activate.
 *
 * No printed mana ability: the chosen basic land type ([GrantChosenSubtype], which reads the
 * land-type choice) gives it that type's intrinsic one (CR 305.6), and
 * [ManaAbilitiesCostAdditionalLife] folds the 1 life into every mana ability it has.
 */
val ThranPortal = card("Thran Portal") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Gate"
    oracleText = "This land enters tapped unless you control two or fewer other lands.\n" +
        "As this land enters, choose a basic land type.\n" +
        "This land is the chosen type in addition to its other types.\n" +
        "Mana abilities of this land cost an additional 1 life to activate."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.YouControlOtherAtMost(2, GameObjectFilter.Land)
    ))

    replacementEffect(EntersWithChoice(ChoiceType.BASIC_LAND_TYPE))

    staticAbility {
        ability = GrantChosenSubtype()
    }

    staticAbility {
        ability = ManaAbilitiesCostAdditionalLife(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Sarah Finnigan"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef074a2e-a387-4af8-a180-74b145d93992.jpg?1783921255"
        ruling("2022-09-09", "Thran Portal will have the appropriate intrinsic mana ability for the basic land type chosen as it enters the battlefield. It is still a Gate and still has its other abilities, including the last ability, which makes the mana ability associated with its basic land type cost 1 life to activate.")
        ruling("2022-09-09", "If Thran Portal somehow gains another mana ability, that ability also costs an additional 1 life to activate.")
        ruling("2022-09-09", "If you control more than one Thran Portal, the last ability of each of them applies only to itself.")
        ruling("2022-09-09", "If a Thran Portal somehow enters the battlefield without a basic land type having been chosen for it, it does not have any mana abilities.")
    }
}
