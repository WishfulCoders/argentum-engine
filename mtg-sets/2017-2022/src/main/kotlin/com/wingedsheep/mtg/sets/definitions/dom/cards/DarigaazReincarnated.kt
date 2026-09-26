package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Darigaaz Reincarnated
 * {4}{B}{R}{G}
 * Legendary Creature — Dragon
 * 7/7
 * Flying, trample, haste
 * If Darigaaz Reincarnated would die, instead exile it with three egg counters on it.
 * At the beginning of your upkeep, if Darigaaz is exiled with an egg counter on it,
 * remove an egg counter from it. Then if it has no egg counters on it, return it to the battlefield.
 */
val DarigaazReincarnated = card("Darigaaz Reincarnated") {
    manaCost = "{4}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Dragon"
    power = 7
    toughness = 7
    oracleText = "Flying, trample, haste\nIf Darigaaz Reincarnated would die, instead exile it with three egg counters on it.\nAt the beginning of your upkeep, if Darigaaz is exiled with an egg counter on it, remove an egg counter from it. Then if Darigaaz has no egg counters on it, return it to the battlefield."

    keywords(Keyword.FLYING, Keyword.TRAMPLE, Keyword.HASTE)

    // If Darigaaz would die, instead exile it with three egg counters
    replacementEffect(
        RedirectZoneChangeWith(
            newDestination = Zone.EXILE,
            additionalEffect = Effects.AddCounters(CounterType.EGG, 3, EffectTarget.Self),
            selfOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Any,
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            )
        )
    )

    // At the beginning of your upkeep, if exiled with an egg counter,
    // remove an egg counter. Then if no egg counters, return to battlefield.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        triggerZone = Zone.EXILE
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.countersOnSelf(CounterType.EGG),
            ComparisonOperator.GTE,
            1
        )
        effect = Effects.RemoveCounters(CounterType.EGG, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.countersOnSelf(CounterType.EGG),
                    ComparisonOperator.EQ,
                    0
                ),
                then = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD)
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "193"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/9459ffca-5a1f-4641-88d4-8a499b261faa.jpg?1562739719"
        ruling("2018-04-27", "If another effect says to exile Darigaaz if it would die, you may apply Darigaaz's own effect first, giving it three egg counters.")
        ruling("2018-04-27", "If Darigaaz is exiled without any egg counters on it, its last ability won't trigger and won't return it to the battlefield.")
    }
}
