package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mardu Roughrider
 * {2}{R}{W}{B}
 * Creature — Orc Warrior
 * 5/4
 * Whenever Mardu Roughrider attacks, target creature can't block this turn.
 */
val MarduRoughrider = card("Mardu Roughrider") {
    manaCost = "{2}{R}{W}{B}"
    colorIdentity = "WBR"
    typeLine = "Creature — Orc Warrior"
    power = 5
    toughness = 4
    oracleText = "Whenever Mardu Roughrider attacks, target creature can't block this turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(target = creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Kev Walker"
        flavorText = "\"The most ferocious saddlebrutes lead the assault, ramming through massed pikes and stout barricades as if they were paper and silk.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6341345-55a6-43f3-915a-c03afea92ec3.jpg?1562794206"
    }
}
