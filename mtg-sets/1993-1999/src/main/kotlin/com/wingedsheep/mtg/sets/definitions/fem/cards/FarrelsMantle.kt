package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Farrel's Mantle
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature attacks and isn't blocked, its controller may have it deal damage
 * equal to its power plus 2 to another target creature. If that player does, the attacking
 * creature assigns no combat damage this turn.
 *
 * The trigger is bound to the enchanted creature (not the Aura), so both the decision and the
 * damage belong to *that* creature's controller — the Mantle can be put on an opponent's creature
 * and it is still their choice. "Another target creature" excludes the attacker itself.
 * [FarrelsZealot] shares the assigns-no-combat-damage rider.
 */
val FarrelsMantle = card("Farrel's Mantle") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature attacks and isn't blocked, its controller may have it deal " +
        "damage equal to its power plus 2 to another target creature. If that player does, the " +
        "attacking creature assigns no combat damage this turn."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.attacksAndIsntBlocked()
        val t = target(TargetFilter(GameObjectFilter.Creature.notAttachedToBySource()))
        effect = Effects.May(
            Effects.DealDamage(
                DynamicAmounts.enchantedCreaturePower() + 2,
                t,
                damageSource = EffectTarget.EnchantedPermanent,
            ) then
                Effects.GrantKeyword(
                    AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE,
                    EffectTarget.EnchantedPermanent,
                    Duration.EndOfTurn,
                ),
            // "Its controller may" — the *enchanted creature's* controller, who need not be the
            // Aura's controller: the Mantle can be put on an opponent's creature and the choice is
            // still theirs. The trigger binds the attacker as the triggering entity for this.
            decisionMaker = EffectTarget.ControllerOfTriggeringEntity,
            descriptionOverride = "have the enchanted creature deal damage equal to its power plus 2 to that creature. If you do, it assigns no combat damage this turn",
        )
        description = "Whenever enchanted creature attacks and isn't blocked, its controller may have it deal damage equal to its power plus 2 to another target creature. If that player does, the attacking creature assigns no combat damage this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af092da3-8713-4a59-86d3-827b942d6456.jpg?1783947921"
    }
}
