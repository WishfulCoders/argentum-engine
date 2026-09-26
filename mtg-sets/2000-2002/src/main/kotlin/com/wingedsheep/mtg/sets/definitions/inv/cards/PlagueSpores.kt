package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Plague Spores
 * {4}{B}{R}
 * Sorcery
 *
 * Destroy target nonblack creature and target land. They can't be regenerated.
 */
val PlagueSpores = card("Plague Spores") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Destroy target nonblack creature and target land. They can't be regenerated."

    spell {
        val creature = target(TargetFilter.Creature.notColor(Color.BLACK))
        val land = target(TargetFilter.Land)
        effect = Effects.CantBeRegenerated(creature) then
            Effects.CantBeRegenerated(land) then
            Effects.Destroy(creature) then
            Effects.Destroy(land)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "260"
        artist = "Randy Gallegos"
        flavorText = "\"Breathe deep, Dominaria. Breathe deep and die.\"\n—Tsabo Tavoc, Phyrexian general"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d106d56-a688-49cc-8d5d-0279a5a7c0a7.jpg?1562897663"
    }
}
