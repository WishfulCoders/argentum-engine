package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Good-Fortune Unicorn
 * {1}{G}{W}
 * Creature — Unicorn
 * 2/2
 * Whenever another creature you control enters, put a +1/+1 counter on that creature.
 *
 * "Another" is `Triggers.another(GameObjectFilter.Creature.youControl()).enters()` (OTHER binding), so the Unicorn's own entry never
 * triggers it. "That creature" is [EffectTarget.TriggeringEntity] — the creature that entered,
 * not the Unicorn.
 */
val GoodFortuneUnicorn = card("Good-Fortune Unicorn") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "WG"
    typeLine = "Creature — Unicorn"
    oracleText = "Whenever another creature you control enters, put a +1/+1 counter on that creature."
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.AddCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            target = EffectTarget.TriggeringEntity
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Kee Lo"
        flavorText = "Catching even a glimpse of one is the start of eight years of good luck."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49d68905-e13e-4751-b028-90c795c11cd5.jpg"
    }
}
