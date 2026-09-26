package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dockworker Drone
 * {1}{W}
 * Artifact Creature — Robot
 * 1/1
 * This creature enters with a +1/+1 counter on it.
 * When this creature dies, put its counters on target creature you control.
 */
val DockworkerDrone = card("Dockworker Drone") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Robot"
    power = 1
    toughness = 1
    oracleText = "This creature enters with a +1/+1 counter on it.\nWhen this creature dies, put its counters on target creature you control."

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true
    ))

    // When this creature dies, put its counters on target creature you control.
    triggeredAbility {
        val creatureYouControl = target(TargetFilter.CreatureYouControl)
        trigger = Triggers.self.dies()
        effect = Effects.MoveAllLastKnownCounters(creatureYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Marco Gorlei"
        flavorText = "Eternity columns are the trade hubs of every solar system."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeff069f-427b-42ad-afb1-36f0e547fb74.jpg?1752946598"
    }
}
