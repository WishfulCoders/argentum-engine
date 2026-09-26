package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Crown of Ascension
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has flying.
 * Sacrifice Crown of Ascension: Enchanted creature and other creatures that share
 * a creature type with it gain flying until end of turn.
 */
val CrownOfAscension = card("Crown of Ascension") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has flying.\nSacrifice Crown of Ascension: Enchanted creature and other creatures that share a creature type with it gain flying until end of turn."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.GrantToEnchantedCreatureTypeGroup(
            keyword = Keyword.FLYING
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Bradley Williams"
        flavorText = "\"Wisdom, clear my eyes.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fe86733-7851-4c2a-8d94-dba6f071b94d.jpg?1562906205"
    }
}
