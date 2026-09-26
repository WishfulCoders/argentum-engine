package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Brambleback Brute
 * {2}{R}
 * Creature — Giant Warrior
 * 4/5
 *
 * This creature enters with two -1/-1 counters on it.
 * {1}{R}, Remove a counter from this creature: Target creature can't block this turn.
 * Activate only as a sorcery.
 */
val BramblebackBrute = card("Brambleback Brute") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Warrior"
    power = 4
    toughness = 5
    oracleText = "This creature enters with two -1/-1 counters on it.\n{1}{R}, Remove a counter from this creature: Target creature can't block this turn. Activate only as a sorcery."

    replacementEffect(EntersWithCounters(
        counterType = CounterType.MINUS_ONE_MINUS_ONE,
        count = 2,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.RemoveCounterFromSelf(CounterType.MINUS_ONE_MINUS_ONE)
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(target = creature)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Aaron Miller"
        flavorText = "Her tangled cape grew with every village razed."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ebb8365-c6e1-46e8-a242-6aa27b21e68a.jpg?1767952109"
    }
}
