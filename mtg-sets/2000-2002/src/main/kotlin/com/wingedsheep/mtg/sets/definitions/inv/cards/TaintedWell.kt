package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetEnchantedLandType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tainted Well
 * {2}{B}
 * Enchantment — Aura
 * Enchant land
 * When this Aura enters, draw a card.
 * Enchanted land is a Swamp.
 */
val TaintedWell = card("Tainted Well") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted land is a Swamp."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = SetEnchantedLandType("Swamp")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Val Mayerik"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2eec00a1-7e12-42d2-8f46-de8ab7323c2c.jpg?1562904562"
    }
}
