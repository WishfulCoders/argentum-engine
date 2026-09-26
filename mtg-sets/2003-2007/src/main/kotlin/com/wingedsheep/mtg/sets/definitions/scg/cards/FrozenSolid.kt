package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Frozen Solid
 * {1}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature doesn't untap during its controller's untap step.
 * When enchanted creature is dealt damage, destroy it.
 */
val FrozenSolid = card("Frozen Solid") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature doesn't untap during its controller's untap step.\nWhen enchanted creature is dealt damage, destroy it."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    triggeredAbility {
        trigger = Triggers.attached.isDealtDamage()
        effect = Effects.Move(
            target = EffectTarget.EnchantedCreature,
            destination = Zone.GRAVEYARD,
            byDestruction = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Glen Angus"
        flavorText = "\"Ice-sheathing keeps a body fresh. And quiet.\"\n—Burg, Mistform illusionist"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b89b98d-0245-4b64-b835-d101ce2bd3fa.jpg?1562532599"
    }
}
