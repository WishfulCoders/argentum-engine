package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Assert Perfection
 * {1}{G}
 * Sorcery
 *
 * Target creature you control gets +1/+0 until end of turn. It deals damage
 * equal to its power to up to one target creature an opponent controls.
 */
val AssertPerfection = card("Assert Perfection") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +1/+0 until end of turn. It deals damage equal to its power to up to one target creature an opponent controls."

    spell {
        val myCreature = target(TargetFilter.CreatureYouControl)
        val theirCreature = target(TargetFilter.CreatureOpponentControls, optional = true)
        effect = Effects.ModifyStats(1, 0, myCreature) then
            Effects.DealDamage(
                amount = DynamicAmounts.powerOf(myCreature),
                target = theirCreature,
                damageSource = myCreature
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Matt Stewart"
        flavorText = "Some elves still cling to the old ways of treating outsiders."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/6995b308-5582-4ca1-ab10-a536d5ca0a6d.jpg?1767732784"
        ruling("2025-11-17", "If either target is an illegal target as Assert Perfection tries to resolve, the creature you control won't deal damage.")
    }
}
