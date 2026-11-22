package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.GrantSupertype
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Super-Soldier Serum — Marvel Super Heroes #38
 * {1}{W} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+2, has first strike and vigilance, and is a legendary Soldier in
 * addition to its other types.
 * Whenever enchanted creature attacks or blocks, attach any number of target Equipment you
 * control to it.
 *
 * The buff line is the On Serra's Wings shape — [ModifyStats], two [GrantKeyword]s, a
 * [GrantSupertype]`("LEGENDARY")` and a [GrantSubtype]`("Soldier")`, every one of them scoped
 * explicitly to [Filters.EnchantedCreature] (GrantSubtype defaults to the *source*, so an Aura
 * that omits the filter would silently make itself a Soldier).
 *
 * The combat ability stays on the Aura: `Triggers.or(attached.attacks(), attached.blocks())`, so
 * "you" is the Aura's controller and "it" is [EffectTarget.EnchantedCreature]. "Any number of
 * target Equipment you control" is a `TargetObject(unlimited = true)` (minimum zero) fanned out
 * with [Effects.ForEachTarget] so each chosen Equipment is attached independently by
 * [Effects.AttachTargetEquipmentToCreature].
 */
val SuperSoldierSerum = card("Super-Soldier Serum") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2, has first strike and vigilance, and is a legendary " +
        "Soldier in addition to its other types.\n" +
        "Whenever enchanted creature attacks or blocks, attach any number of target Equipment " +
        "you control to it."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantSupertype("LEGENDARY", Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantSubtype("Soldier", Filters.EnchantedCreature)
    }

    triggeredAbility {
        trigger = Triggers.or(Triggers.attached.attacks(), Triggers.attached.blocks())
        target = TargetObject(
            filter = TargetFilter(baseFilter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
            unlimited = true,
        )
        effect = Effects.ForEachTarget(
            Effects.AttachTargetEquipmentToCreature(
                equipmentTarget = EffectTarget.ContextTarget(0),
                creatureTarget = EffectTarget.EnchantedCreature,
            )
        )
        description = "Whenever enchanted creature attacks or blocks, attach any number of target " +
            "Equipment you control to it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Rafater"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/845b0be1-4f85-4a8c-8205-dc85c8cf9a61.jpg?1783902965"
    }
}
