package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trade Route Envoy — Tarkir: Dragonstorm #163
 * {3}{G} · Creature — Dog Soldier · 4/3
 *
 * When this creature enters, draw a card if you control a creature with a counter on it.
 * If you don't draw a card this way, put a +1/+1 counter on this creature.
 *
 * Modeled as a single ETB [Effects.If]: if you control a creature with any counter
 * ([Conditions.YouControl] over a creature filter carrying [StatePredicate.HasAnyCounter]),
 * draw a card; otherwise put a +1/+1 counter on Trade Route Envoy itself. The condition is
 * checked at resolution, so this creature's own counter (e.g. one it gained earlier) counts.
 */
val TradeRouteEnvoy = card("Trade Route Envoy") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dog Soldier"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, draw a card if you control a creature with a counter on it. " +
        "If you don't draw a card this way, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.YouControl(
                GameObjectFilter.Creature.copy(
                    statePredicates = listOf(StatePredicate.HasAnyCounter)
                )
            ),
            then = Effects.DrawCards(1),
            otherwise = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
        description = "When this creature enters, draw a card if you control a creature with a counter on it. " +
            "If you don't draw a card this way, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Gaboleps"
        flavorText = "The Abzan pride themselves on the safety of their roads. " +
            "To reject their escorts invites storm and sorrow."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0c89d95-d697-4cfa-9dfa-52d7adb96176.jpg?1743204618"
    }
}
