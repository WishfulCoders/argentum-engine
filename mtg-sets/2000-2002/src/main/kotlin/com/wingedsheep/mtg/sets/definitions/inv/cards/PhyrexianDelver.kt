package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phyrexian Delver
 * {3}{B}{B}
 * Creature — Phyrexian Zombie
 * 3/2
 * When this creature enters, return target creature card from your graveyard to the
 * battlefield. You lose life equal to that card's mana value.
 */
val PhyrexianDelver = card("Phyrexian Delver") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Zombie"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, return target creature card from your graveyard " +
        "to the battlefield. You lose life equal to that card's mana value."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val returned = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(returned, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.LoseLife(
                DynamicAmounts.manaValueOf(returned),
                EffectTarget.Controller,
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Dana Knutson"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e66d87a5-7b67-4ec5-b5e2-518d67123118.jpg?1562941267"
    }
}
