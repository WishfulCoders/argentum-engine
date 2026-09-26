package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ethereal Armor
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 for each enchantment you control and has first strike.
 */
val EtherealArmor = card("Ethereal Armor") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+1 for each enchantment you control and has first strike."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        val enchantmentCount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Enchantment).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = enchantmentCount,
            toughnessBonus = enchantmentCount
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Tyler Walpole"
        flavorText = "Clad in the light of conviction and hope, Linette strode forth to face Duskmourn's terrors without fear."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd0e8a82-0201-483e-a976-5f29764b35a4.jpg?1726285881"
    }
}
