package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Guilty Conscience
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature deals damage, Guilty Conscience deals that much damage to that creature.
 */
val GuiltyConscience = card("Guilty Conscience") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhenever enchanted creature deals damage, Guilty Conscience deals that much damage to that creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dealsDamage()
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.EnchantedCreature,
            damageSource = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Christopher Moeller"
        flavorText = "\"Those who most feel guilt don't feel need to, while those who most need to feel guilt never do.\" —Order proverb"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67b8701c-0f03-4ad0-9097-3caf885abd59.jpg?1562529779"
    }
}
