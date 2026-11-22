package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tamiyo's Compleation — Kamigawa: Neon Dynasty #83 (canonical printing)
 * {3}{U} · Enchantment — Aura
 *
 * Flash
 * Enchant artifact, creature, or planeswalker
 * When this Aura enters, tap enchanted permanent. If it's an Equipment, unattach it.
 * Enchanted permanent loses all abilities and doesn't untap during its controller's untap step.
 *
 * Stop Cold's shape with Planar Disruption's enchant restriction plus the Equipment rider: the
 * enter trigger re-reads the enchanted permanent at resolution, taps it, and — if it's an
 * Equipment (a subtype, so losing all abilities doesn't hide it) — unattaches it from whatever it
 * equips. A reconfigure Equipment keeps its "not a creature while attached" effect until that
 * unattach happens, per the rulings.
 */
val TamiyosCompleation = card("Tamiyo's Compleation") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant artifact, creature, or planeswalker\n" +
        "When this Aura enters, tap enchanted permanent. If it's an Equipment, unattach it.\n" +
        "Enchanted permanent loses all abilities and doesn't untap during its controller's untap step."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(
        filter = TargetFilter(
            GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Planeswalker
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedPermanent) then Effects.If(
            Conditions.EnchantedPermanentMatches(GameObjectFilter.Artifact.withSubtype("Equipment")),
            Effects.UnattachEquipment(EffectTarget.EnchantedPermanent),
            descriptionOverride = "If it's an Equipment, unattach it"
        )
    }

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aee62e43-6a82-4749-972b-e21802642abf.jpg?1783923893"
        ruling("2022-02-18", "An Equipment creature that loses its reconfigure ability cannot be attached to a creature by any effect.")
        ruling("2022-02-18", "An attached Equipment with reconfigure that loses its reconfigure ability doesn't become a creature again until it becomes unattached, and it won't become unattached until the triggered ability of Tamiyo's Compleation resolves. If that ability is countered, the Equipment will stay attached to the equipped creature.")
        ruling("2022-02-18", "A planeswalker with Tamiyo's Compleation attached to it may still be attacked and it will continue to lose loyalty counters if damage is dealt to it.")
    }
}
