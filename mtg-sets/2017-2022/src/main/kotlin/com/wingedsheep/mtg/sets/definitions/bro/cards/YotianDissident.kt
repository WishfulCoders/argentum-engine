package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Yotian Dissident
 * {G}{W}
 * Creature — Human Artificer
 * 1/1
 * Whenever an artifact you control enters, put a +1/+1 counter on target creature you control.
 *
 * The shared BRO "artifact you control enters" trigger — `Triggers.a(filter).enters()` over
 * `Artifact.youControl()` with [TriggerBinding.ANY] — feeding [Effects.AddCounters] on a
 * declared target slot restricted to creatures you control.
 */
val YotianDissident = card("Yotian Dissident") {
    manaCost = "{G}{W}"
    colorIdentity = "WG"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 1
    oracleText = "Whenever an artifact you control enters, put a +1/+1 counter on target creature you control."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Put a +1/+1 counter on target creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Steve Prescott"
        flavorText = "\"I joined this war to protect my homeland, not to destroy someone else's.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1eb02f00-c188-4193-a049-d26f7643e5da.jpg?1783920022"
    }
}
