package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arbor Armament
 * {G}
 * Instant
 * Put a +1/+1 counter on target creature. That creature gains reach until end of turn.
 */
val ArborArmament = card("Arbor Armament") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature. That creature gains reach until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t) then
            Effects.GrantKeyword(Keyword.REACH, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Bayard Wu"
        flavorText = "\"Llanowar's boughs are ever ready / To unleash an autumn of steel leaves.\" —\"Song of Freyalise\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bceb365c-5de6-47ae-b42d-7fbce7781f8e.jpg?1615334447"
    }
}
