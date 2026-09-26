package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Savage Smash
 * {1}{R}{G}
 * Sorcery
 * Target creature you control gets +2/+2 until end of turn. It fights target creature you don't
 * control.
 *
 * Savage Punch's shape without the Ferocious condition. Both targets are required (2019-01-25
 * ruling); the pump lands on a still-legal creature of yours even if the other target is gone, and
 * the fight needs both.
 */
val SavageSmash = card("Savage Smash") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +2/+2 until end of turn. It fights target creature you don't control. " +
        "(Each deals damage equal to its power to the other.)"

    spell {
        val yours = target(TargetObject(
            filter = TargetFilter(GameObjectFilter.Creature.youControl())
        ))
        val theirs = target(TargetObject(
            filter = TargetFilter(GameObjectFilter.Creature.opponentControls())
        ))
        effect = Effects.ModifyStats(2, 2, yours).then(Effects.Fight(yours, theirs))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Zoltan Boros"
        flavorText = "A Gruul berserker is never unarmed."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ca942d7-a3a3-429f-a159-fc2363d9bca6.jpg?1783933639"
        ruling("2019-01-25", "You can't cast Savage Smash unless you choose both a creature you control and a creature you don't control as targets.")
        ruling("2019-01-25", "If either target is an illegal target as Savage Smash resolves, neither creature will deal or be dealt damage.")
        ruling("2019-01-25", "If the creature you control is an illegal target as Savage Smash tries to resolve, it won't get +2/+2. If that creature is a legal target but the other creature isn't, the creature you control still gets +2/+2.")
    }
}
