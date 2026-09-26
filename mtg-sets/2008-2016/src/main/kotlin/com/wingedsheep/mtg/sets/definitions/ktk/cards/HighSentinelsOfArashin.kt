package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * High Sentinels of Arashin
 * {3}{W}
 * Creature — Bird Soldier
 * 3/4
 * Flying
 * High Sentinels of Arashin gets +1/+1 for each other creature you control with a +1/+1 counter on it.
 * {3}{W}: Put a +1/+1 counter on target creature.
 */
val HighSentinelsOfArashin = card("High Sentinels of Arashin") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 4
    oracleText = "Flying\nHigh Sentinels of Arashin gets +1/+1 for each other creature you control with a +1/+1 counter on it.\n{3}{W}: Put a +1/+1 counter on target creature."

    keywords(Keyword.FLYING)

    // Gets +1/+1 for each other creature you control with a +1/+1 counter on it
    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE),
                excludeSelf = true
            ).count(),
            toughnessBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE),
                excludeSelf = true
            ).count()
        )
    }

    // {3}{W}: Put a +1/+1 counter on target creature
    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            target = t
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db5f4bab-f918-4b42-b82c-cfcf5ff0c58a.jpg?1562794547"
    }
}
