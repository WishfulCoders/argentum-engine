package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Cryoshatter
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -5/-0.
 * When enchanted creature becomes tapped or is dealt damage, destroy it.
 */
val Cryoshatter = card("Cryoshatter") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -5/-0.\nWhen enchanted creature becomes tapped or is dealt damage, destroy it."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-5, 0, GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.attached.becomesTapped()
        effect = Effects.Move(
            target = EffectTarget.EnchantedCreature,
            destination = Zone.GRAVEYARD,
            byDestruction = true
        )
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
        collectorNumber = "53"
        artist = "Jeremy Wilson"
        flavorText = "Illvoi are masters of the deep freeze, though conditions must be perfect to avoid going to pieces."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b62b1e2-9e43-4a66-a647-7e5de2871f2a.jpg?1752946762"
    }
}
