package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mossborn Hydra
 * {2}{G}
 * Creature — Elemental Hydra
 * 0/0
 * Trample
 * This creature enters with a +1/+1 counter on it.
 * Landfall — Whenever a land you control enters, double the number of +1/+1 counters on this creature.
 */
val MossbornHydra = card("Mossborn Hydra") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental Hydra"
    power = 0
    toughness = 0
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\nThis creature enters with a +1/+1 counter on it.\nLandfall — Whenever a land you control enters, double the number of +1/+1 counters on this creature."

    keywords(Keyword.TRAMPLE)

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true
    ))

    // Landfall: double +1/+1 counters on this creature
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Monztre"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7054a0d7-396f-40b4-ab24-db591c3b08f0.jpg?1730488992"
    }
}
