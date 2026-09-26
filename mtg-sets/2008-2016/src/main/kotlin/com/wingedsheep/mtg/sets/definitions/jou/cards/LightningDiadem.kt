package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lightning Diadem
 * {5}{R}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, it deals 2 damage to any target.
 * Enchanted creature gets +2/+2.
 */
val LightningDiadem = card("Lightning Diadem") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, it deals 2 damage to any target.\nEnchanted creature gets +2/+2."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(2, victim)
        description = "When this Aura enters, it deals 2 damage to any target."
    }

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Ryan Alexander Lee"
        flavorText = "\"I fight for Keranos and for mortals. I see no contradiction.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ce3f1c6-2e6f-4087-8619-a01fdcc6d4a3.jpg?1783939422"
    }
}
