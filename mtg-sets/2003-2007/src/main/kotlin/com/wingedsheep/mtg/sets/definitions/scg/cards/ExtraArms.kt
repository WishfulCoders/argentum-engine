package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Extra Arms
 * {4}{R}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature attacks, it deals 2 damage to any target.
 */
val ExtraArms = card("Extra Arms") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhenever enchanted creature attacks, it deals 2 damage to any target."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        val any = target(Targets.Any)
        effect = Effects.DealDamage(
            amount = 2,
            target = any,
            damageSource = EffectTarget.EnchantedCreature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Greg Staples"
        flavorText = "\"Perhaps extra heads would have served them better.\" —Foothill guide"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28efa11c-6aeb-4c22-bbb3-b41f26d65c65.jpg?1562526643"
    }
}
