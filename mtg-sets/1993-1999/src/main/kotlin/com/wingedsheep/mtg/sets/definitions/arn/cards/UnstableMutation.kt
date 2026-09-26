package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Unstable Mutation
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +3/+3.
 * At the beginning of the upkeep of enchanted creature's controller, put a -1/-1 counter on that creature.
 */
val UnstableMutation = card("Unstable Mutation") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +3/+3.\nAt the beginning of the upkeep of enchanted creature's controller, put a -1/-1 counter on that creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 3, Filters.EnchantedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a79e9236-a39e-471a-b18a-2c2ba16e7774.jpg?1562926311"
    }
}
