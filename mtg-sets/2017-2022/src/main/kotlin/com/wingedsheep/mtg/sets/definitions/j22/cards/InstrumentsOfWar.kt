package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Instruments of War
 * {4}
 * Artifact
 *
 * Flash
 * As this artifact enters, choose a creature type.
 * Creatures you control of the chosen type get +1/+1.
 */
val InstrumentsOfWar = card("Instruments of War") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Flash\nAs this artifact enters, choose a creature type.\nCreatures you control of the chosen type get +1/+1."

    keywords(Keyword.FLASH)

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.ChosenSubtypeCreatures().youControl()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Drew Tucker"
        flavorText = "Perfect for keeping time at a battle or a ball."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9526bea2-8d47-4a82-a617-6c57c7abc69a.jpg?1783919175"
    }
}
