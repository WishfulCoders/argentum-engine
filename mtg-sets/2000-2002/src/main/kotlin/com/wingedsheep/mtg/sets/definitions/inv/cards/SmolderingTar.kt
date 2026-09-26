package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Smoldering Tar
 * {2}{B}{R}
 * Enchantment
 * At the beginning of your upkeep, target player loses 1 life.
 * Sacrifice this enchantment: It deals 4 damage to target creature. Activate only as a sorcery.
 */
val SmolderingTar = card("Smoldering Tar") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, target player loses 1 life.\n" +
        "Sacrifice this enchantment: It deals 4 damage to target creature. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val player = target(Targets.Player)
        effect = Effects.LoseLife(1, player)
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature)
        description = "Sacrifice this enchantment: It deals 4 damage to target creature. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "275"
        artist = "David Day"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcdc55c0-c8ac-49d5-969b-9bf0ee8e696c.jpg?1562946036"
    }
}
