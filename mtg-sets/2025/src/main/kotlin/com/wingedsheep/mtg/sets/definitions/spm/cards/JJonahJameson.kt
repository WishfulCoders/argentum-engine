package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * J. Jonah Jameson
 * {2}{R}
 * Legendary Creature — Human Citizen
 * 2/2
 *
 * When J. Jonah Jameson enters, suspect up to one target creature.
 * Whenever a creature you control with menace attacks, create a Treasure token.
 */
val JJonahJameson = card("J. Jonah Jameson") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Citizen"
    power = 2
    toughness = 2
    oracleText = "When J. Jonah Jameson enters, suspect up to one target creature. " +
        "(A suspected creature has menace and can't block.)\n" +
        "Whenever a creature you control with menace attacks, create a Treasure token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature, optional = true)
        effect = Effects.Suspect(t)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withKeyword(Keyword.MENACE).youControl()).attacks()
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Paolo Rivera"
        flavorText = "\"The photos are garbage, Parker! I'll buy them all.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ee905d6-b647-4eb1-a8d9-89add9bafc31.jpg?1761565998"
    }
}
