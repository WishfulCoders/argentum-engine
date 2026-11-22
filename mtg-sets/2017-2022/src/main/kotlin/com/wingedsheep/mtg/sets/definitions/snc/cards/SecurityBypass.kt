package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Security Bypass — Streets of New Capenna #59 (canonical printing, only printing)
 * {1}{U} · Enchantment — Aura
 *
 * Enchant creature
 * As long as enchanted creature is attacking alone, it can't be blocked.
 * Enchanted creature has "Whenever this creature deals combat damage to a player, it connives."
 *
 * The evasion clause is the Gutter Shortcut shape: [CantBeBlocked] over
 * [GroupFilter.attachedCreature] gated by `Conditions.EnchantedPermanentMatches(attackingAlone())`,
 * re-checked continuously during projection.
 *
 * The connive clause is granted to the creature (the Synapse Sliver / Instill Furor shape), so the
 * granted trigger is controlled by the creature's controller and `EffectTarget.Self` is the
 * enchanted creature — the creature that connives and receives the +1/+1 counter.
 */
val SecurityBypass = card("Security Bypass") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "As long as enchanted creature is attacking alone, it can't be blocked.\n" +
        "Enchanted creature has \"Whenever this creature deals combat damage to a player, it connives.\" " +
        "(Its controller draws a card, then discards a card. If they discarded a nonland card, they put a " +
        "+1/+1 counter on this creature.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantBeBlocked(filter = GroupFilter.attachedCreature())
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Any.attackingAlone())
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.Connive(EffectTarget.Self),
                descriptionOverride = "Whenever this creature deals combat damage to a player, it connives.",
            ),
            filter = GroupFilter.attachedCreature(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c155be9-8432-4e35-b586-96b6339d27ae.jpg?1783923140"
    }
}
