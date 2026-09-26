package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Teyo's Lightshield
 * {2}{W}
 * Creature — Illusion
 * 0/3
 * When this creature enters, put a +1/+1 counter on target creature you control.
 *
 * A 0/3 body that hands its enters trigger's counter to something that can use it — the target
 * may be the Lightshield itself, since "target creature you control" carries no other-than-source
 * restriction.
 */
val TeyosLightshield = card("Teyo's Lightshield") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Illusion"
    oracleText = "When this creature enters, put a +1/+1 counter on target creature you control."
    power = 0
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Igor Kieryluk"
        flavorText = "Teyo gifted his shields to as many allies as possible, knowing he could not always be there to protect them himself."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfffe235-98b1-43db-9461-1b2da5f0690e.jpg"
    }
}
