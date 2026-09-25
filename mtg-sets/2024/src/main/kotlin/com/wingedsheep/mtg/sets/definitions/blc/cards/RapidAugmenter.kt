package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rapid Augmenter — Bloomburrow Commander #38
 * {1}{U}{R} · Creature — Otter Artificer · 1/3
 *
 * Haste
 * Whenever another creature you control with base power 1 enters, it gains haste until end of turn.
 * Whenever another creature you control enters, if it wasn't cast, put a +1/+1 counter on this
 * creature and this creature can't be blocked this turn.
 *
 * Offspring tokens and other token makers trip both abilities at once: a 1/1 token has base power
 * 1 and wasn't cast. "If it wasn't cast" is an intervening-if on the entering creature's cast
 * record.
 */
val RapidAugmenter = card("Rapid Augmenter") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Otter Artificer"
    power = 1
    toughness = 3
    oracleText = "Haste\n" +
        "Whenever another creature you control with base power 1 enters, it gains haste until end of turn.\n" +
        "Whenever another creature you control enters, if it wasn't cast, put a +1/+1 counter on this creature " +
        "and this creature can't be blocked this turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().basePower(1)).enters()
        effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.TriggeringEntity)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        interveningIf = Conditions.Not(Conditions.TriggeringEntityWasCast)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4643565-a462-45ce-b5ed-f19b796a64c5.jpg?1783910726"
        ruling(
            "2024-07-26",
            "Normally, a creature's base power is the power printed on the card or, for a token, the power set " +
                "by the effect that created it. If an effect modifies a creature's power without setting it, that " +
                "is not included when determining its base power."
        )
    }
}
