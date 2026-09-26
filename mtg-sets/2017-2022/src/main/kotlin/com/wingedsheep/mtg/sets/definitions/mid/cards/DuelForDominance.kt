package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Duel for Dominance
 * {1}{G}
 * Instant
 * Coven — Choose target creature you control and target creature you don't control. If you control
 * three or more creatures with different powers, put a +1/+1 counter on the chosen creature you
 * control. Then the chosen creatures fight each other.
 *
 * Coven is an ability word (no keyword). The condition is realized as
 * [Aggregation.DISTINCT_VALUES] over [CardNumericProperty.POWER] among creatures you control ≥ 3
 * ([Conditions.CompareAmounts]), checked at resolution. The counter is gated on that condition; the
 * fight always happens (Longstalk Brawl target/fight shape).
 */
val DuelForDominance = card("Duel for Dominance") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Coven — Choose target creature you control and target creature you don't control. " +
        "If you control three or more creatures with different powers, put a +1/+1 counter on the " +
        "chosen creature you control. Then the chosen creatures fight each other. (Each deals damage " +
        "equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(TargetFilter.CreatureOpponentControls)

        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                left = DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Creature,
                ).distinctValues(CardNumericProperty.POWER),
                operator = ComparisonOperator.GTE,
                right = 3,
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, mine),
        ) then Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bb0a0c8-158e-4a6f-976e-3de9f57c3463.jpg?1782703611"
    }
}
