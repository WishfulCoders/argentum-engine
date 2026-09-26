package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rabid Gnaw
 * {1}{R}
 * Instant
 *
 * Target creature you control gets +1/+0 until end of turn. Then it deals
 * damage equal to its power to target creature you don't control.
 */
val RabidGnaw = card("Rabid Gnaw") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +1/+0 until end of turn. Then it deals damage equal to its power to target creature you don't control."

    spell {
        val myCreature = target(TargetFilter.CreatureYouControl)
        val theirCreature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(1, 0, myCreature) then
            Effects.DealDamage(
                amount = DynamicAmounts.powerOf(myCreature),
                target = theirCreature,
                damageSource = myCreature
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Mark Behm"
        flavorText = "Unbeknownst to the hosts, the foraged seeds they served at dinner had been touched by a Calamity Beast. The results were ... disturbing."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f815bae-820a-49f6-8eed-46f658e7b6ff.jpg?1721426681"
        ruling("2024-07-26", "If the creature you control is a legal target but the creature you don't control isn't as Rabid Gnaw resolves, the creature you control will get +1/+0 until end of turn, but no damage will be dealt. If instead the creature you control is an illegal target but the creature you don't control is still a legal target, nothing will happen when Rabid Gnaw resolves.")
    }
}
