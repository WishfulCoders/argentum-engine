package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

// Absolute difference between the triggering creature's power and toughness:
// max(toughness - power, power - toughness).
private val TriggeringPower: DynamicAmount =
    DynamicAmounts.triggeringPower()
private val TriggeringToughness: DynamicAmount =
    DynamicAmounts.triggeringToughness()
private val TriggeringPowerToughnessDifference: DynamicAmount = DynamicAmounts.max(
    TriggeringToughness - TriggeringPower,
    TriggeringPower - TriggeringToughness
)

/**
 * Doran, Besieged by Time
 * {1}{W}{B}{G}
 * Legendary Creature — Treefolk Druid
 * 0/5
 *
 * Each creature spell you cast with toughness greater than its power costs {1} less to cast.
 * Whenever a creature you control attacks or blocks, it gets +X/+X until end of turn,
 * where X is the difference between its power and toughness.
 */
val DoranBesiegedByTime = card("Doran, Besieged by Time") {
    manaCost = "{1}{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Legendary Creature — Treefolk Druid"
    power = 0
    toughness = 5
    oracleText = "Each creature spell you cast with toughness greater than its power costs {1} less to cast.\n" +
        "Whenever a creature you control attacks or blocks, it gets +X/+X until end of turn, where X is the difference between its power and toughness."

    // Each creature spell you cast with toughness greater than its power costs {1} less to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature.toughnessGreaterThanPower()),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    // Whenever a creature you control attacks, it gets +X/+X until end of turn,
    // where X is the difference between its power and toughness.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        effect = Effects.ModifyStats(
            power = TriggeringPowerToughnessDifference,
            toughness = TriggeringPowerToughnessDifference,
            target = EffectTarget.TriggeringEntity
        )
    }

    // Whenever a creature you control blocks, same.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).blocks()
        effect = Effects.ModifyStats(
            power = TriggeringPowerToughnessDifference,
            toughness = TriggeringPowerToughnessDifference,
            target = EffectTarget.TriggeringEntity
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Carl Critchlow"
        flavorText = "\"Each year that passes rots you with scars. Shelter your heart, for the world is cruel.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/568aa70a-6765-486a-bd37-5d38b16c46de.jpg?1767732894"

        ruling("2025-11-17", "The value of X is calculated only once, as Doran's last ability resolves.")
        ruling("2025-11-17", "Doran's first ability applies only to generic mana in the total cost of creature spells you cast with toughness greater than their power.")
        ruling("2025-11-17", "To find the difference between a creature's power and its toughness, subtract the smaller of those two numbers from the larger one. For example, the difference between the power and toughness of a 3/5 creature is 2. The difference between the power and toughness of a 5/3 creature is also 2.")
    }
}
