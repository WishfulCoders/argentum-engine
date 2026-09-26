package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dragon Fangs
 * {1}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 and has trample.
 * When a creature with mana value 6 or greater enters, you may return Dragon Fangs
 * from your graveyard to the battlefield attached to that creature.
 */
val DragonFangs = card("Dragon Fangs") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+1 and has trample.\nWhen a creature with mana value 6 or greater enters, you may return Dragon Fangs from your graveyard to the battlefield attached to that creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.manaValueAtLeast(6)).enters()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.May(
            effect = Effects.ReturnSelfToBattlefieldAttached(),
            descriptionOverride = "Attach Dragon Fangs to this creature?",
            sourceRequiredZone = Zone.GRAVEYARD,
            inlineOnTrigger = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9754f52f-8937-4402-8956-2c18b520898a.jpg?1562532554"
    }
}
