package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Chosen by Heliod
 * {1}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, draw a card.
 * Enchanted creature gets +0/+2.
 */
val ChosenByHeliod = card("Chosen by Heliod") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, draw a card.\nEnchanted creature gets +0/+2."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = ModifyStats(0, 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Zack Stella"
        flavorText = "\"Training and studies aid a soldier in meager amounts. The gods do the rest.\"\n—Brigone, soldier of Meletis"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8ead499-920c-465a-a23d-f08710d7e9bc.jpg"
    }
}
