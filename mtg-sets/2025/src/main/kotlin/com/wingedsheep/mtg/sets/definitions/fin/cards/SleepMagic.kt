package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sleep Magic
 * {U}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 * When enchanted creature is dealt damage, sacrifice this Aura.
 *
 * Same lock as Charmed Sleep, plus a "released on damage" trigger: the ATTACHED-bound
 * `takesDamage` trigger fires when the enchanted creature is dealt damage and sacrifices the Aura,
 * freeing the creature to untap again.
 */
val SleepMagic = card("Sleep Magic") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step.\n" +
        "When enchanted creature is dealt damage, sacrifice this Aura."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    triggeredAbility {
        trigger = Triggers.attached.isDealtDamage()
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Le Vuong"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c96cae63-7625-48e3-aaba-5b1632a8642d.jpg?1748706038"
    }
}
