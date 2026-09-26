package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lavamancer's Skill
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "{T}: This creature deals 1 damage to target creature."
 * As long as enchanted creature is a Wizard, it has "{T}: This creature deals 2 damage
 * to target creature." instead.
 *
 * Implementation uses a single granted ability with conditional damage amount (2 if Wizard, 1 otherwise).
 */
val LavamancersSkill = card("Lavamancer's Skill") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has \"{T}: This creature deals 1 damage to target creature.\"\nAs long as enchanted creature is a Wizard, it has \"{T}: This creature deals 2 damage to target creature.\" instead."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val creature = target(TargetFilter.Creature)
                effect = Effects.DealDamage(
                    amount = DynamicAmounts.conditional(
                        condition = EnchantedCreatureHasSubtype(Subtype("Wizard")),
                        ifTrue = 2,
                        ifFalse = 1
                    ),
                    target = creature,
                    damageSource = EffectTarget.Self
                )
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "215"
        artist = "Scott M. Fischer"
        flavorText = "The best wizards know the shortest route between two points is a bolt of fire."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d4dd156-a2c1-4fab-b9f4-3302a4e8835a.jpg?1562898074"
    }
}
