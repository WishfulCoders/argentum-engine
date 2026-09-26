package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Reluctant Dounguard
 * {2}{W}
 * Creature — Kithkin Soldier
 * 4/4
 *
 * This creature enters with two -1/-1 counters on it.
 * Whenever another creature you control enters while this creature has a -1/-1 counter on it,
 * remove a -1/-1 counter from this creature.
 */
val ReluctantDounguard = card("Reluctant Dounguard") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kithkin Soldier"
    power = 4
    toughness = 4
    oracleText = "This creature enters with two -1/-1 counters on it.\n" +
        "Whenever another creature you control enters while this creature has a -1/-1 counter on it, " +
        "remove a -1/-1 counter from this creature."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.MINUS_ONE_MINUS_ONE,
            count = 2,
            selfOnly = true,
        )
    )

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        triggerRestriction = Conditions.SourceHasCounter(CounterType.MINUS_ONE_MINUS_ONE)
        effect = Effects.RemoveCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Paolo Parente"
        flavorText = "Keeping others out of the doun is the only thing that motivates some to step outside their walls."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbce93c0-5efc-4b60-9cef-9d9b374d397b.jpg?1767871722"
    }
}
