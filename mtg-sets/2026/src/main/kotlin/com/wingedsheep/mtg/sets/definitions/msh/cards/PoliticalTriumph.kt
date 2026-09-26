package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Political Triumph — Marvel Super Heroes #31
 * {W} · Enchantment — Plan
 *
 * Whenever a creature you control enters, scry 1 and put a plan counter on this enchantment.
 * When the fourth plan counter is put on this enchantment, sacrifice it, draw a card, and put a
 * +1/+1 counter on each creature you control.
 *
 * Modeling notes:
 *  - The accumulator is an ANY-bound enters trigger over `GameObjectFilter.Creature.youControl()`
 *    (the enchantment itself is not a creature, so no OTHER binding is needed).
 *  - "When the **fourth** plan counter is put on this enchantment" composes from existing
 *    vocabulary: a SELF-bound `Triggers.<subject>.getsCounters(type, by, firstTimeEachTurn, batch)` on [CounterType.PLAN] gated by
 *    `triggerRestriction = `[Conditions.SourceCounterCountAtLeast]`(PLAN, 4)`. The at-least gate is
 *    behaviourally exact for this cycle because the payoff **sacrifices its own source**, so the
 *    enchantment is gone before a fifth counter could ever land — the threshold can never fire
 *    twice. No dedicated "Nth counter" trigger event is needed.
 *  - "A +1/+1 counter on each creature you control" is [Effects.ForEachInGroup] over a
 *    [GroupFilter] with the counter applied to `EffectTarget.IterationEntity` per iterated creature (the
 *    Cathars' Crusade shape) — not a target.
 */
val PoliticalTriumph = card("Political Triumph") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Plan"
    oracleText = "Whenever a creature you control enters, scry 1 and put a plan counter on this " +
        "enchantment.\n" +
        "When the fourth plan counter is put on this enchantment, sacrifice it, draw a card, and " +
        "put a +1/+1 counter on each creature you control."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.Scry(1) then Effects.AddCounters(CounterType.PLAN, 1, EffectTarget.Self)
        description = "Whenever a creature you control enters, scry 1 and put a plan counter on " +
            "this enchantment."
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLAN)
        triggerRestriction = Conditions.SourceCounterCountAtLeast(CounterType.PLAN, 4)
        effect = Effects.SacrificeTarget(EffectTarget.Self) then
            Effects.DrawCards(1) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl()),
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
            )
        description = "When the fourth plan counter is put on this enchantment, sacrifice it, " +
            "draw a card, and put a +1/+1 counter on each creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Monztre"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dec3dd36-b8ca-432b-8973-d37c6efc4c1a.jpg?1783902968"
    }
}
