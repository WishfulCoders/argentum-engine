package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sleeper's Robe
 * {U}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has fear.
 * Whenever enchanted creature deals combat damage to an opponent, you may draw a card.
 */
val SleepersRobe = card("Sleeper's Robe") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has fear. (It can't be blocked except by artifact creatures and/or black creatures.)\n" +
        "Whenever enchanted creature deals combat damage to an opponent, you may draw a card."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.FEAR, GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.Opponent)
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "273"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3411f0fd-8b85-4d0d-a202-701a24ffac9f.jpg?1562905482"
    }
}
