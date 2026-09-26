package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sheltering Boughs
 * {2}{G}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, draw a card.
 * Enchanted creature gets +1/+3.
 */
val ShelteringBoughs = card("Sheltering Boughs") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted creature gets +1/+3."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = ModifyStats(1, 3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "218"
        artist = "Anato Finnstark"
        flavorText = "\"We're not at odds with the woods. How could we be, when we share so many enemies?\"\n—Marel, Dawnhart witch"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/915dd4c2-0e9f-440c-8e4c-80db351a5eba.jpg?1782703040"
    }
}
