package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Combat Research
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "Whenever this creature deals combat damage to a player, draw a card."
 * As long as enchanted creature is legendary, it gets +1/+1 and has ward {1}.
 */
val CombatResearch = card("Combat Research") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has \"Whenever this creature deals combat damage to a player, draw a card.\"\nAs long as enchanted creature is legendary, it gets +1/+1 and has ward {1}."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantTriggeredAbility(
            TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.DrawCards(1)
            )
        )
    }

    staticAbility {
        ability = ModifyStats(1, 1)
        condition = Conditions.EnchantedCreatureIsLegendary()
    }

    staticAbility {
        ability = GrantWard(WardCost.Mana("{1}"))
        condition = Conditions.EnchantedCreatureIsLegendary()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c44738c-706f-40b2-a09d-b21cd0889049.jpg?1673306670"
    }
}
