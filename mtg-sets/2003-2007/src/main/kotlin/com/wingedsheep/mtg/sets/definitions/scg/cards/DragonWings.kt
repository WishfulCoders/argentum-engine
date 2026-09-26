package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dragon Wings
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has flying.
 * Cycling {1}{U}
 * When a creature with mana value 6 or greater enters, you may return Dragon Wings
 * from your graveyard to the battlefield attached to that creature.
 */
val DragonWings = card("Dragon Wings") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has flying.\nCycling {1}{U}\nWhen a creature with mana value 6 or greater enters, you may return Dragon Wings from your graveyard to the battlefield attached to that creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.attachedCreature())
    }

    keywordAbility(KeywordAbility.cycling("{1}{U}"))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.manaValueAtLeast(6)).enters()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.May(
            effect = Effects.ReturnSelfToBattlefieldAttached(),
            descriptionOverride = "Attach Dragon Wings to this creature?",
            sourceRequiredZone = Zone.GRAVEYARD,
            inlineOnTrigger = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7674ab4d-9bc0-45c3-88e1-3fd2c947cfaa.jpg?1562530706"
    }
}
