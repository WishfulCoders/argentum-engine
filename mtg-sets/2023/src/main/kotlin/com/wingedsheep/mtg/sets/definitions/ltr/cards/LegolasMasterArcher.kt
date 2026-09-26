package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Legolas, Master Archer
 * {1}{G}{G}
 * Legendary Creature — Elf Archer
 * 1/4
 *
 * Reach
 * Whenever you cast a spell that targets Legolas, put a +1/+1 counter on Legolas.
 * Whenever you cast a spell that targets a creature you don't control, Legolas deals
 * damage equal to its power to up to one target creature.
 *
 * Both abilities use the `SpellCastPredicate.TargetsSource` / `TargetsMatching` cast-time
 * predicates (Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource)) / youCastSpellTargeting). "A creature you
 * don't control" is `Not(ControlledByYou)` (not `opponentControls()`, which would miss a
 * teammate's creatures in multiplayer). The damage clause composes `DealDamage(sourcePower(),
 * target)` — the damage source defaults to Legolas (the trigger source).
 */
val LegolasMasterArcher = card("Legolas, Master Archer") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Archer"
    power = 1
    toughness = 4
    oracleText = "Reach\n" +
        "Whenever you cast a spell that targets Legolas, put a +1/+1 counter on Legolas.\n" +
        "Whenever you cast a spell that targets a creature you don't control, Legolas deals " +
        "damage equal to its power to up to one target creature."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Creature.withControllerPredicate(
                ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
            ))))
        val t = target(TargetFilter.Creature, optional = true)
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9405577-c1dc-48e0-b2aa-6237c569d02e.jpg?1686969439"
    }
}
