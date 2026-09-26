package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bathe in Dragonfire
 * {2}{R}
 * Sorcery
 *
 * Bathe in Dragonfire deals 4 damage to target creature.
 */
val BatheInDragonfire = card("Bathe in Dragonfire") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Bathe in Dragonfire deals 4 damage to target creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Chris Rallis"
        flavorText = "The scent of cooked flesh lingers in the charred landscape of Tarkir."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8cc6931-2005-4d0a-a42a-ce8bc279372e.jpg?1783938691"
    }
}
