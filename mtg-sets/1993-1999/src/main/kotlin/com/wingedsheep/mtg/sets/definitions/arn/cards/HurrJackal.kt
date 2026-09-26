package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hurr Jackal
 * {R}
 * Creature — Jackal
 * 1/1
 * {T}: Target creature can't be regenerated this turn.
 */
val HurrJackal = card("Hurr Jackal") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Jackal"
    power = 1
    toughness = 1
    oracleText = "{T}: Target creature can't be regenerated this turn."

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBeRegenerated(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4aadda8-8577-480d-8186-532d2b173c15.jpg?1562940974"
    }
}
