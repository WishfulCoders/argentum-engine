package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dwarven Nomad
 * {2}{R}
 * Creature — Dwarf Nomad
 * 1/1
 * {T}: Target creature with power 2 or less can't be blocked this turn.
 */
val DwarvenNomad = card("Dwarven Nomad") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Nomad"
    power = 1
    toughness = 1
    oracleText = "{T}: Target creature with power 2 or less can't be blocked this turn."

    activatedAbility {
        cost = Costs.Tap
        val t = target(TargetFilter.Creature.powerAtMost(2))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Mike Kimble"
        flavorText = "A dwarf's life belongs as much to the land as to the dwarf.\n—Creed of the Stonethrow Clan"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30b09e65-5e69-48f8-be9b-a1e9706f18bf.jpg?1562718333"
    }
}
