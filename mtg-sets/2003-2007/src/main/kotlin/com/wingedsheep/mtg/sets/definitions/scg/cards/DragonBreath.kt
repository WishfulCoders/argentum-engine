package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dragon Breath
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has haste.
 * {R}: Enchanted creature gets +1/+0 until end of turn.
 * When a creature with mana value 6 or greater enters, you may return Dragon Breath
 * from your graveyard to the battlefield attached to that creature.
 */
val DragonBreath = card("Dragon Breath") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has haste.\n{R}: Enchanted creature gets +1/+0 until end of turn.\nWhen a creature with mana value 6 or greater enters, you may return Dragon Breath from your graveyard to the battlefield attached to that creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.attachedCreature())
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.EnchantedCreature)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.manaValueAtLeast(6)).enters()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.May(
            effect = Effects.ReturnSelfToBattlefieldAttached(),
            descriptionOverride = "Attach Dragon Breath to this creature?",
            sourceRequiredZone = Zone.GRAVEYARD,
            inlineOnTrigger = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/1832aaed-e164-4f78-9bc9-ec6c015835f5.jpg?1562526058"
    }
}
