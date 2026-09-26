package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wakandan Royal Guard — Marvel Super Heroes #195
 * {4}{G} · Creature — Human Soldier Hero · Common
 * 4/4
 *
 * Vigilance
 * When this creature enters, put a +1/+1 counter on target creature. If that creature is
 * another Hero, put two +1/+1 counters on it instead.
 *
 * The "instead" clause is a resolution-time branch on the chosen target, not a replacement
 * effect: a [Effects.If] that puts two counters when the target is a Hero *other than*
 * this creature (the Guard is itself a Hero, so targeting itself takes the one-counter branch —
 * hence [Conditions.TargetIsSource] under [Conditions.Not]).
 */
val WakandanRoyalGuard = card("Wakandan Royal Guard") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Soldier Hero"
    power = 4
    toughness = 4
    oracleText = "Vigilance\n" +
        "When this creature enters, put a +1/+1 counter on target creature. If that creature " +
        "is another Hero, put two +1/+1 counters on it instead."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.All(
                Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.HERO), creature),
                Conditions.Not(Conditions.TargetIsSource())
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature),
            otherwise = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        )
        description = "When this creature enters, put a +1/+1 counter on target creature. If " +
            "that creature is another Hero, put two +1/+1 counters on it instead."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Randy Gallegos"
        flavorText = "The shields and spears of Wakanda are forever united in defense of their king."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bf0b1e9-32b5-4591-8dd2-6b5f9768c72d.jpg?1783902908"
    }
}
