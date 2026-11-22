package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TapSymbolInCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Imprison — {B} Enchantment — Aura.
 *
 * Both abilities stay on the Aura (ATTACHED binding), so "you" is the Aura's controller and
 * "destroy this Aura" is [EffectTarget.Self]:
 *
 * - The activation half is `Triggers.attached.hasAbilityActivated(tapInCost = WITH)` under the
 *   default "isn't a mana ability" gate; its triggering entity is the activated ability on the
 *   stack, which [Effects.CounterTriggeringAbility] counters.
 * - The combat half is `Triggers.or(attacks, blocks)` on the enchanted creature, then tap it and
 *   remove it from combat with `unblockSoleBlockedAttackers` — the attackers only it was blocking
 *   become unblocked (the same clause as Ydwen Efreet and False Orders).
 */
val Imprison = card("Imprison") {
    manaCost = "{B}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever a player activates an ability of enchanted creature with {T} in its activation cost " +
        "that isn't a mana ability, you may pay {1}. If you do, counter that ability. If you don't, " +
        "destroy this Aura.\n" +
        "Whenever enchanted creature attacks or blocks, you may pay {1}. If you do, tap the creature, " +
        "remove it from combat, and creatures it was blocking that had become blocked by only that " +
        "creature this combat become unblocked. If you don't, destroy this Aura."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.hasAbilityActivated(tapInCost = TapSymbolInCost.WITH)
        effect = Effects.MayPay(
            cost = Effects.PayMana("{1}"),
            then = Effects.CounterTriggeringAbility(),
            otherwise = Effects.Destroy(EffectTarget.Self),
            descriptionOverride = "Pay {1}? If you do, counter that ability. If you don't, destroy Imprison."
        )
        description = "Whenever a player activates an ability of enchanted creature with {T} in its " +
            "activation cost that isn't a mana ability, you may pay {1}. If you do, counter that " +
            "ability. If you don't, destroy this Aura."
    }

    triggeredAbility {
        trigger = Triggers.or(Triggers.attached.attacks(), Triggers.attached.blocks())
        effect = Effects.MayPay(
            cost = Effects.PayMana("{1}"),
            then = Effects.Tap(EffectTarget.EnchantedCreature) then
                Effects.RemoveFromCombat(EffectTarget.EnchantedCreature, unblockSoleBlockedAttackers = true),
            otherwise = Effects.Destroy(EffectTarget.Self),
            descriptionOverride = "Pay {1}? If you do, tap the creature and remove it from combat. " +
                "If you don't, destroy Imprison."
        )
        description = "Whenever enchanted creature attacks or blocks, you may pay {1}. If you do, tap " +
            "the creature, remove it from combat, and creatures it was blocking that had become " +
            "blocked by only that creature this combat become unblocked. If you don't, destroy this Aura."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12671381-beb7-41b8-9484-97f8aca5c981.jpg?1783948065"
    }
}
