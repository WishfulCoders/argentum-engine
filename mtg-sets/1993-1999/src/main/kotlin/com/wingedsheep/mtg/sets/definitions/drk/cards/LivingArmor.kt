package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Living Armor
 * {4}
 * Artifact
 * {T}, Sacrifice this artifact: Put X +0/+1 counters on target creature, where X is that
 * creature's mana value.
 *
 * X reads the *target's* mana value, not the Armor's, so it is
 * `EntityProperty(ContextTarget(0), ManaValue)` rather than anything sourced off the artifact. Mana value
 * is the printed cost (CR 202.3), so a cost reduction that let the creature be cast cheaply still
 * yields the full number of counters, and a token with no mana cost yields none.
 */
val LivingArmor = card("Living Armor") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice this artifact: Put X +0/+1 counters on target creature, " +
        "where X is that creature's mana value."

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ZERO_PLUS_ONE,
            DynamicAmounts.manaValueOf(creature),
            creature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Anson Maddocks"
        flavorText = "Though it affords excellent protection, few don this armor. The process is " +
            "uncomfortable and not easily reversed."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c31a957-ad1e-40cc-b3c4-2f4caa492b77.jpg?1783947925"
    }
}
