package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spike Drone
 * {G}
 * Creature — Spike Drone
 * 0/0
 * This creature enters with a +1/+1 counter on it.
 * {2}, Remove a +1/+1 counter from this creature: Put a +1/+1 counter on target creature.
 */
val SpikeDrone = card("Spike Drone") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spike Drone"
    power = 0
    toughness = 0
    oracleText = "This creature enters with a +1/+1 counter on it.\n" +
        "{2}, Remove a +1/+1 counter from this creature: Put a +1/+1 counter on target creature."

    replacementEffect(EntersWithCounters(count = 1, selfOnly = true))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 1)
        )
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "258"
        artist = "Charles Gillespie"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d45a3d3-a114-496e-b575-504179a297cc.jpg"
    }
}
