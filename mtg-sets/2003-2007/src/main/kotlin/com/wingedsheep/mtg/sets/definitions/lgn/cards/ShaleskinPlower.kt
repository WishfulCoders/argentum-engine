package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shaleskin Plower
 * {3}{R}
 * Creature — Beast
 * 3/2
 * Morph {4}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When this creature is turned face up, destroy target land.
 */
val ShaleskinPlower = card("Shaleskin Plower") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 2
    oracleText = "Morph {4}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, destroy target land."

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        val t = target(TargetFilter.Land)
        effect = Effects.Destroy(t)
    }

    morph = "{4}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42658b33-9a12-403b-bc7d-807fbe1f1a36.jpg?1562908348"
    }
}
