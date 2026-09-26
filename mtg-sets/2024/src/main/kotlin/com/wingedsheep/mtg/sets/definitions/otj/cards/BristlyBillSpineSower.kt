package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bristly Bill, Spine Sower
 * {1}{G}
 * Legendary Creature — Plant Druid
 * 2/2
 *
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on target creature.
 * {3}{G}{G}: Double the number of +1/+1 counters on each creature you control.
 */
val BristlyBillSpineSower = card("Bristly Bill, Spine Sower") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Plant Druid"
    power = 2
    toughness = 2
    oracleText = "Landfall — Whenever a land you control enters, put a +1/+1 counter on target creature.\n{3}{G}{G}: Double the number of +1/+1 counters on each creature you control."

    // Landfall: whenever a land you control enters, put a +1/+1 counter on target creature
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    // {3}{G}{G}: Double the number of +1/+1 counters on each creature you control
    activatedAbility {
        cost = Costs.Mana("{3}{G}{G}")
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddDynamicCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmounts.countersOn(EffectTarget.IterationEntity, CounterType.PLUS_ONE_PLUS_ONE),
                target = EffectTarget.IterationEntity
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "157"
        artist = "Daniel Zrom"
        flavorText = "\"Wake up, little ones. There's a new day ahead.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52eef0d6-24b7-40b7-8403-e8e863d0cd55.jpg?1712355894"
    }
}
