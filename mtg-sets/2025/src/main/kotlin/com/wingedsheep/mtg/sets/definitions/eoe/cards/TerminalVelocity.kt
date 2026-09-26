package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Terminal Velocity {4}{R}{R}
 * Sorcery
 *
 * You may put an artifact or creature card from your hand onto the battlefield. That
 * permanent gains haste, "When this permanent leaves the battlefield, it deals damage
 * equal to its mana value to each creature," and "At the beginning of your end step,
 * sacrifice this permanent."
 *
 * The two quoted clauses are granted as real [TriggeredAbility]s on the chosen permanent
 * (Duration.Permanent), not as delayed triggers anchored to the resolving sorcery. That
 * keeps them rules-faithful: the LTB damage reads the permanent's last-known mana value
 * via [EffectTarget.Self], and the end-step sacrifice fires every "your end step"
 * for as long as the permanent persists (e.g. if the sacrifice trigger is countered, the
 * permanent keeps all three granted abilities — including the LTB clause).
 */
val TerminalVelocity = card("Terminal Velocity") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "You may put an artifact or creature card from your hand onto the battlefield. " +
        "That permanent gains haste, \"When this permanent leaves the battlefield, it deals damage " +
        "equal to its mana value to each creature,\" and \"At the beginning of your end step, " +
        "sacrifice this permanent.\""

    spell {
        val ltbDamage = TriggeredAbility.create(
            trigger = Triggers.self.leaves(),
            effect = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreatures,
                effect = Effects.DealDamage(
                    amount = DynamicAmounts.sourceManaValue(),
                    target = EffectTarget.IterationEntity,
                ),
            ),
            descriptionOverride = "When this permanent leaves the battlefield, it deals damage equal to its mana value to each creature.",
        )

        val endStepSacrifice = TriggeredAbility.create(
            trigger = Triggers.you.beginningOf(Step.END),
            effect = Effects.SacrificeTarget(target = EffectTarget.Self),
            descriptionOverride = "At the beginning of your end step, sacrifice this permanent.",
        )

        effect = Effects.Pipeline {
            run(Patterns.Hand.putFromHand(filter = GameObjectFilter.Artifact or GameObjectFilter.Creature))
            val put = Patterns.Hand.putFromHandCards
            ifNotEmpty(put) {
                run(Effects.GrantKeyword(keyword = Keyword.HASTE, target = put.asTarget, duration = Duration.Permanent))
                run(Effects.GrantTriggeredAbility(ability = ltbDamage, target = put.asTarget, duration = Duration.Permanent))
                run(Effects.GrantTriggeredAbility(ability = endStepSacrifice, target = put.asTarget, duration = Duration.Permanent))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Xabi Gaztelua"
        flavorText = "A victory was the best retirement gift the admiral could have asked for."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d18dc06-16f0-4a3b-8d52-dbf4aa2c393d.jpg?1752947211"
        ruling("2025-07-25", "Use the permanent's mana value as it last existed on the battlefield to determine how much damage the triggered ability deals.")
        ruling("2025-07-25", "If a permanent has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
    }
}
