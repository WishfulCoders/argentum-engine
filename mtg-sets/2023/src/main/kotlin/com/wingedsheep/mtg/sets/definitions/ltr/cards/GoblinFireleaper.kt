package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Goblin Fireleaper
 * {1}{R}
 * Creature — Goblin Warrior
 * 1/1
 * {1}{R}: This creature gets +1/+0 until end of turn.
 * When this creature dies, it deals damage equal to its power to target creature an opponent controls.
 */
val GoblinFireleaper = card("Goblin Fireleaper") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "{1}{R}: This creature gets +1/+0 until end of turn.\nWhen this creature dies, it deals damage equal to its power to target creature an opponent controls."

    // {1}{R}: This creature gets +1/+0 until end of turn.
    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    // When this creature dies, it deals damage equal to its power to target creature an opponent controls.
    triggeredAbility {
        trigger = Triggers.self.dies()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Javier Charro"
        flavorText = "\"Let us hope that fire now lies between us and pursuit. Come! There is no time to lose.\"\n—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec120dae-8c40-4053-9341-1f7774464634.jpg?1686969004"
    }
}
