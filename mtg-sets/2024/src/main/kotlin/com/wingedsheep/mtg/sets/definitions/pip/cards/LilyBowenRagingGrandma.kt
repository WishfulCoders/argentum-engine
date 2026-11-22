package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lily Bowen, Raging Grandma
 * {3}{G}
 * Legendary Creature — Mutant Warrior
 * 0/0
 *
 * The "if its power is 16 or less" is not an intervening-if (it doesn't follow the trigger
 * condition), so it is checked once, as the ability resolves, and picks a branch.
 *
 * Doubling is "put as many +1/+1 counters as it already has" ([Effects.DoubleCounters]), so
 * counter-placement replacements (Branching Evolution, Hardened Scales) apply per the ruling.
 *
 * "Otherwise" branch: the +1/+1 count is stored before the removal, all but one is removed
 * (clamped at zero when there are none), and the life gained is the stored count minus the count
 * left afterwards — exactly the counters actually "removed this way", zero if Lily has left.
 */
val LilyBowenRagingGrandma = card("Lily Bowen, Raging Grandma") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Mutant Warrior"
    power = 0
    toughness = 0
    oracleText = "Vigilance\n" +
        "Lily Bowen enters with two +1/+1 counters on it.\n" +
        "At the beginning of your upkeep, double the number of +1/+1 counters on Lily Bowen if its " +
        "power is 16 or less. Otherwise, remove all but one +1/+1 counter from it, then you gain 1 " +
        "life for each +1/+1 counter removed this way."

    keywords(Keyword.VIGILANCE)

    replacementEffect(
        EntersWithCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 2, selfOnly = true)
    )

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(DynamicAmounts.sourcePower(), ComparisonOperator.LTE, 16),
            then = Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.Self),
            otherwise = Effects.StoreNumber(
                "lilyBowenCountersBefore",
                DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE)
            ) then Effects.RemoveCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.nonNegative(DynamicAmounts.storedNumber("lilyBowenCountersBefore") - 1),
                EffectTarget.Self
            ) then Effects.GainLife(
                DynamicAmounts.nonNegative(
                    DynamicAmounts.storedNumber("lilyBowenCountersBefore") -
                        DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE)
                )
            )
        )
        description = "At the beginning of your upkeep, double the number of +1/+1 counters on Lily Bowen " +
            "if its power is 16 or less. Otherwise, remove all but one +1/+1 counter from it, then you " +
            "gain 1 life for each +1/+1 counter removed this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Villarrte"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/052dc548-d356-43fe-9484-665db7e1f505.jpg?1783912398"
        ruling(
            "2024-03-08",
            "To double the number of +1/+1 counters on Lily Bowen, put a number of +1/+1 counters on it " +
                "equal to the number it already has. Replacement effects that modify the number of counters " +
                "being placed on creatures you control, such as the effect of Branching Evolution, apply to " +
                "this ability as normal."
        )
    }
}
