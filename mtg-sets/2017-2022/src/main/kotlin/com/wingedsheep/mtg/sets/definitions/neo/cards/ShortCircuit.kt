package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Short Circuit — Kamigawa: Neon Dynasty #78
 * {1}{U} · Enchantment — Aura
 *
 * Flash
 * Enchant artifact or creature
 * As long as enchanted permanent is a creature, it gets -3/-0 and loses flying.
 *
 * Favor of Jukai's shape: both halves are gated by [Conditions.EnchantedPermanentMatches] on the
 * host's current (projected) type, so an uncrewed Vehicle is unaffected until it becomes a creature.
 */
val ShortCircuit = card("Short Circuit") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant artifact or creature\n" +
        "As long as enchanted permanent is a creature, it gets -3/-0 and loses flying."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = ModifyStats(-3, 0, GroupFilter.attachedCreature())
    }

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = RemoveKeywordStatic(Keyword.FLYING, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74fe0fb5-e994-41b1-a73d-4e68f7afdf34.jpg?1783923894"
    }
}
