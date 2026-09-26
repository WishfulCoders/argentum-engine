package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Dawn-Blessed Pennant
 * {1}
 * Artifact
 *
 * As this artifact enters, choose Elemental, Elf, Faerie, Giant, Goblin, Kithkin, Merfolk, or Treefolk.
 * Whenever a permanent you control of the chosen type enters, you gain 1 life.
 * {2}, {T}, Sacrifice this artifact: Return target card of the chosen type from your graveyard to your hand.
 */
val DawnBlessedPennant = card("Dawn-Blessed Pennant") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText =
        "As this artifact enters, choose Elemental, Elf, Faerie, Giant, Goblin, Kithkin, Merfolk, or Treefolk.\n" +
        "Whenever a permanent you control of the chosen type enters, you gain 1 life.\n" +
        "{2}, {T}, Sacrifice this artifact: Return target card of the chosen type from your graveyard to your hand."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CREATURE_TYPE,
            allowedCreatureTypes = listOf(
                "Elemental", "Elf", "Faerie", "Giant",
                "Goblin", "Kithkin", "Merfolk", "Treefolk"
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.youControl().withChosenSubtype()).enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        val card = target(TargetFilter(GameObjectFilter.Any.ownedByYou().withChosenSubtype(), zone = Zone.GRAVEYARD))
        effect = Effects.ReturnToHand(card)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "254"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/294266b6-0343-4fa5-90b8-0adf7df490e4.jpg?1767658603"
    }
}
