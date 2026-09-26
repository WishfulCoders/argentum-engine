package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blight Rot
 * {2}{B}
 * Instant
 *
 * Put four -1/-1 counters on target creature.
 */
val BlightRot = card("Blight Rot") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Put four -1/-1 counters on target creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 4, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Forrest Schehl"
        flavorText = "The scratch became a sore, the sore became a wound, the wound became an infection, and the infection became a funeral."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5201bdeb-ba47-459b-ac0d-603367914578.jpg?1767952066"
    }
}
