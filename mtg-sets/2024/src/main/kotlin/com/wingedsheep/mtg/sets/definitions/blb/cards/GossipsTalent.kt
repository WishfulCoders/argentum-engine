package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gossip's Talent
 * {1}{U}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * Whenever a creature you control enters, surveil 1.
 *
 * {1}{U}: Level 2
 * Whenever you attack, target attacking creature with power 3 or less can't be blocked
 * this turn.
 *
 * {3}{U}: Level 3
 * Whenever a creature you control deals combat damage to a player, you may exile it,
 * then return it to the battlefield under its owner's control.
 */
val GossipsTalent = card("Gossip's Talent") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\nWhenever a creature you control enters, surveil 1.\n{1}{U}: Level 2\nWhenever you attack, target attacking creature with power 3 or less can't be blocked this turn.\n{3}{U}: Level 3\nWhenever a creature you control deals combat damage to a player, you may exile it, then return it to the battlefield under its owner's control."

    // Level 1: Whenever a creature you control enters, surveil 1
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Patterns.Library.surveil(1)
    }

    // Level 2: Whenever you attack, target attacking creature with power 3 or less
    // can't be blocked this turn
    classLevel(2, "{1}{U}") {
        triggeredAbility {
            trigger = Triggers.you.attacks()
            val creature = target(TargetFilter(GameObjectFilter.Creature.attacking().powerAtMost(3)))
            effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
        }
    }

    // Level 3: Whenever a creature you control deals combat damage to a player,
    // you may exile it, then return it to the battlefield under its owner's control
    classLevel(3, "{3}{U}") {
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
            effect = Effects.May(
                Effects.Move(EffectTarget.TriggeringEntity, Zone.EXILE) then
                    Effects.Move(EffectTarget.TriggeringEntity, Zone.BATTLEFIELD)
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Andrea Sipl"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b299889a-03d6-4659-b0e1-f0830842e40f.jpg?1721426095"
        ruling("2024-07-26", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2024-07-26", "You can't activate the first level ability of a Class unless that Class is level 1. You can't activate the second level ability of a Class unless that Class is level 2.")
        ruling("2024-07-26", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
