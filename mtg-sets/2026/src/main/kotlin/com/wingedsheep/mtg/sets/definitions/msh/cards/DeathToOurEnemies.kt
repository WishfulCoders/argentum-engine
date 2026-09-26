package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Death to Our Enemies — Marvel Super Heroes #127
 * {2}{R} · Enchantment — Plan
 *
 * Whenever you cast a noncreature spell, create a tapped Treasure token and put a plan counter on
 * this enchantment.
 * When the fourth plan counter is put on this enchantment, sacrifice it. When you do, it deals 7
 * damage divided as you choose among one or two targets.
 *
 * Modeling notes:
 *  - "When the **fourth** plan counter is put on this enchantment" composes from existing
 *    vocabulary: a SELF-bound `Triggers.<subject>.getsCounters(type, by, firstTimeEachTurn, batch)` on [CounterType.PLAN] gated by
 *    `triggerRestriction = `[Conditions.SourceCounterCountAtLeast]`(PLAN, 4)`. The at-least gate is
 *    behaviourally exact here because the payoff **sacrifices its own source**, so the enchantment
 *    is gone before a fifth counter could ever land — the threshold can never fire twice. No
 *    dedicated "Nth counter" trigger event is needed.
 *  - "Sacrifice it. When you do, …" is a mandatory [ReflexiveTriggerEffect] (`optional = false`).
 *    The division is [Effects.DividedDamage]`(7, 1, 2)` over an `AnyTarget(count = 2, minCount = 1)`
 *    requirement carried by the reflexive ability, so the one-or-two targets and the split are
 *    locked in as that second stack object is put on the stack (CR 601.2d / 603.12) — after the
 *    enchantment has already been sacrificed, which is why the damage source is last-known
 *    information rather than a permanent on the battlefield.
 */
val DeathToOurEnemies = card("Death to Our Enemies") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Plan"
    oracleText = "Whenever you cast a noncreature spell, create a tapped Treasure token and put a " +
        "plan counter on this enchantment.\n" +
        "When the fourth plan counter is put on this enchantment, sacrifice it. When you do, it " +
        "deals 7 damage divided as you choose among one or two targets."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.CreateTreasure(1, tapped = true) then
            Effects.AddCounters(CounterType.PLAN, 1, EffectTarget.Self)
        description = "Whenever you cast a noncreature spell, create a tapped Treasure token and " +
            "put a plan counter on this enchantment."
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLAN)
        triggerRestriction = Conditions.SourceCounterCountAtLeast(CounterType.PLAN, 4)
        effect = Effects.ReflexiveTrigger(
            action = Effects.SacrificeTarget(EffectTarget.Self),
            optional = false,
            reflexiveEffect = Effects.DividedDamage(total = 7, minTargets = 1, maxTargets = 2),
            reflexiveTargetRequirements = listOf(AnyTarget(count = 2, minCount = 1)),
            descriptionOverride = "Sacrifice this enchantment. When you do, it deals 7 damage " +
                "divided as you choose among one or two targets.",
        )
        description = "When the fourth plan counter is put on this enchantment, sacrifice it. " +
            "When you do, it deals 7 damage divided as you choose among one or two targets."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Lee Woo-chul"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2a8f518-c0b5-4e15-aab2-49b5ef29fb41.jpg?1783902933"
    }
}
