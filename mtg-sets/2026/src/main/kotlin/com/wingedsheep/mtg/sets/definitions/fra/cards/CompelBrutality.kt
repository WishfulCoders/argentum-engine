package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Compel Brutality
 * {1}{G}
 * Instant
 *
 * Choose one —
 * • Target creature you control deals damage equal to its power to target creature or planeswalker
 *   an opponent controls.
 * • Target planeswalker you control deals damage equal to its loyalty to target creature or
 *   planeswalker an opponent controls.
 *
 * Both modes are the one-sided "bite": the first target is the damage source, the amount is read
 * off it at resolution. A planeswalker's loyalty on the battlefield is the number of loyalty
 * counters on it, so the second mode's amount is that counter count.
 */
val CompelBrutality = card("Compel Brutality") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature you control deals damage equal to its power to target creature or " +
        "planeswalker an opponent controls.\n" +
        "• Target planeswalker you control deals damage equal to its loyalty to target creature " +
        "or planeswalker an opponent controls."

    spell {
        modal(chooseCount = 1) {
            mode(
                "Target creature you control deals damage equal to its power to target creature " +
                    "or planeswalker an opponent controls"
            ) {
                val yours = target(TargetFilter.Creature.youControl())
                val theirs = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
                effect = Effects.DealDamage(
                    amount = DynamicAmounts.powerOf(yours),
                    target = theirs,
                    damageSource = yours,
                )
            }

            mode(
                "Target planeswalker you control deals damage equal to its loyalty to target " +
                    "creature or planeswalker an opponent controls"
            ) {
                val yours = target(TargetFilter.Planeswalker.youControl())
                val theirs = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
                effect = Effects.DealDamage(
                    amount = DynamicAmounts.countersOn(yours, CounterType.LOYALTY),
                    target = theirs,
                    damageSource = yours,
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Danny Schwartz"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd32d736-7a58-46b9-90b4-2cac3c3e80a1.jpg?1788521235"
        inBooster = false
    }
}
