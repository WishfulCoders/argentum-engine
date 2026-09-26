package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spontaneous Mutation
 * {U}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Enchanted creature gets -X/-0, where X is the number of cards in your graveyard.
 *
 * The power penalty is a continuously recomputed [GrantDynamicStats] on the attached creature:
 * -(cards in your graveyard) power, +0 toughness. Modeled after Exotic Curse's negative dynamic buff.
 */
val SpontaneousMutation = card("Spontaneous Mutation") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets -X/-0, where X is the number of cards in your graveyard."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = -DynamicAmounts.cardsInYourGraveyard(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a022c983-0fb8-4e8c-9ba3-9356ad340f66.jpg?1782711899"
    }
}
