package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Step

/**
 * Pizza Face, Gastromancer
 * {3}{B}{G}
 * Legendary Artifact Creature — Food Mutant
 * 2/4
 *
 * When Pizza Face enters, create a Food token.
 * Disappear — At the beginning of your end step, if a permanent left the
 * battlefield under your control this turn, put three +1/+1 counters on up to
 * one other target artifact or creature. If it isn't a creature, it becomes a
 * 0/0 Mutant creature in addition to its other types.
 * {10}, {T}, Sacrifice Pizza Face: You gain 15 life.
 */
val PizzaFaceGastromancer = card("Pizza Face, Gastromancer") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Artifact Creature — Food Mutant"
    oracleText = "When Pizza Face enters, create a Food token.\nDisappear — At the beginning of your end step, if a permanent left the battlefield under your control this turn, put three +1/+1 counters on up to one other target artifact or creature. If it isn't a creature, it becomes a 0/0 Mutant creature in addition to its other types.\n{10}, {T}, Sacrifice Pizza Face: You gain 15 life."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateFood()
        description = "When Pizza Face enters, create a Food token."
    }

    // Disappear — three +1/+1 counters on up to one other target artifact or creature;
    // a noncreature target also becomes a 0/0 Mutant creature in addition to its other
    // types (same conditional-BecomeCreature idiom as Brilliance Unleashed).
    triggeredAbility {
        val target = target(TargetFilter.CreatureOrArtifact.copy(excludeSelf = true), optional = true)
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, target) then
            Effects.If(
                condition = Conditions.Not(
                    Conditions.TargetMatchesFilter(GameObjectFilter.Creature, target)
                ),
                then = Effects.BecomeCreature(
                    target = target,
                    power = 0,
                    toughness = 0,
                    creatureTypes = setOf("Mutant"),
                    duration = Duration.Permanent
                )
            )
        description = "Disappear — At the beginning of your end step, if a permanent left the battlefield under your control this turn, put three +1/+1 counters on up to one other target artifact or creature. If it isn't a creature, it becomes a 0/0 Mutant creature in addition to its other types."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{10}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Effects.GainLife(15)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Villarrte"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b03cf0bb-3207-4e8e-bb3f-e3e4367aa86e.jpg?1771599896"
    }
}
