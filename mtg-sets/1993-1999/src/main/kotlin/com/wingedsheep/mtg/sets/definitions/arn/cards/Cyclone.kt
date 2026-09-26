package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Cyclone
 * {2}{G}{G}
 * Enchantment
 * At the beginning of your upkeep, put a wind counter on this enchantment, then sacrifice this
 * enchantment unless you pay {G} for each wind counter on it. If you pay, this enchantment deals
 * damage equal to the number of wind counters on it to each creature and each player.
 *
 * Composition:
 *  - Each upkeep adds a wind counter (passive [CounterType.WIND]), then the pay-or-sacrifice is an
 *    `Effects.MayPay` (Gate.MayPay): pay {G} per wind counter (a colored dynamic mana cost via
 *    `Effects.PayDynamicMana(..., color = GREEN)`) → deal damage; decline → sacrifice.
 *  - Both the cost amount and the damage scale off `DynamicAmounts.countersOnSelf(WIND)`, evaluated
 *    after the counter is added so they see the incremented total (CR 608.2c sequencing).
 *  - The damage hits each creature (`ForEachInGroup(AllCreatures, …)`) and each player.
 */
val Cyclone = card("Cyclone") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, put a wind counter on this enchantment, then " +
        "sacrifice this enchantment unless you pay {G} for each wind counter on it. If you pay, " +
        "this enchantment deals damage equal to the number of wind counters on it to each creature " +
        "and each player."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)

        val windCount = DynamicAmounts.countersOnSelf(CounterType.WIND)

        val dealDamageToAll = Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.DealDamage(windCount, EffectTarget.IterationEntity)
        ) then
            Effects.ForEachPlayer(Player.Each, listOf(Effects.DealDamage(windCount, EffectTarget.Controller)))

        effect = Effects.AddCounters(CounterType.WIND, 1, EffectTarget.Self) then
            Effects.MayPay(
                cost = Effects.PayDynamicMana(windCount, color = Color.GREEN),
                then = dealDamageToAll,
                otherwise = Effects.SacrificeTarget(EffectTarget.Self)
            )
        description = "At the beginning of your upkeep, put a wind counter on this enchantment, then " +
            "sacrifice this enchantment unless you pay {G} for each wind counter on it. If you pay, " +
            "this enchantment deals damage equal to the number of wind counters on it to each creature and each player."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f11684d6-5b74-47a7-a2d0-256c9e437aa6.jpg?1562940349"
    }
}
