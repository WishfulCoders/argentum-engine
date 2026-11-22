package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Dungeon Map
 * {3}
 * Artifact
 * {T}: Add {C}.
 * {3}, {T}: Venture into the dungeon. Activate only as a sorcery.
 */
val DungeonMap = card("Dungeon Map") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n" +
        "{3}, {T}: Venture into the dungeon. Activate only as a sorcery. (Enter the first room or advance to the next room.)"

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.VentureIntoTheDungeon()
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "Aaron Miller"
        flavorText = "\"Wait, is that a secret door? I can't tell for sure . . . there's some blood in the way.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/028506ad-ad36-44e5-b488-d038e2d2445f.jpg?1783926440"
        ruling("2021-07-23", "Choosing the dungeon or room to venture into is part of resolving the venture into the dungeon keyword action. Once that choice is made, players may not respond until after the appropriate room ability has triggered.")
    }
}
