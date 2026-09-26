package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Beastie Beatdown
 * {R}{G}
 * Sorcery
 * Choose target creature you control and target creature an opponent controls.
 * Delirium — If there are four or more card types among cards in your graveyard, put two
 * +1/+1 counters on the creature you control.
 * The creature you control deals damage equal to its power to the creature an opponent controls.
 *
 * One-sided "fight": only the controlled creature deals damage. The Delirium counters are placed
 * before the damage step so the (possibly buffed) power is used. `targetPower(0)` reads the first
 * declared target (the controlled creature, also the damage source).
 */
val BeastieBeatdown = card("Beastie Beatdown") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Choose target creature you control and target creature an opponent controls.\n" +
        "Delirium — If there are four or more card types among cards in your graveyard, put two " +
        "+1/+1 counters on the creature you control.\n" +
        "The creature you control deals damage equal to its power to the creature an opponent controls."

    spell {
        val yours = target(TargetFilter.Creature.youControl())
        val theirs = target(TargetFilter.Creature.opponentControls())
        // Delirium — counters land first so the damage uses the buffed power.
        effect = Effects.If(
            condition = Conditions.Delirium(),
            then = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 2, target = yours),
        ) then
            Effects.DealDamage(DynamicAmounts.powerOf(yours), theirs, damageSource = yours)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Inkognit"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f889c95-46af-4fd9-aff2-573d5384fd58.jpg?1726286652"
    }
}
