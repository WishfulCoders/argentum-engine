package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Earth Kingdom Soldier
 * {4}{G/W}
 * Creature — Human Soldier
 * 3/4
 *
 * Vigilance
 * When this creature enters, put a +1/+1 counter on each of up to two target creatures you control.
 *
 * The ETB ability targets "up to two target creatures you control" as a single optional
 * [TargetCreature] requirement (count = 2, optional) restricted to creatures you control.
 * [ForEachTargetEffect] applies a [Effects.AddCounters] of one +1/+1 counter to each chosen
 * target ([EffectTarget.ContextTarget]), so zero, one, or two creatures may be chosen.
 */
val EarthKingdomSoldier = card("Earth Kingdom Soldier") {
    manaCost = "{4}{G/W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 4
    oracleText = "Vigilance\n" +
        "When this creature enters, put a +1/+1 counter on each of up to two target creatures you control."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.Creature.youControl(), count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
        description = "When this creature enters, put a +1/+1 counter on each of up to two target creatures you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "216"
        artist = "Rafater"
        flavorText = "Trained to be steady as the earth beneath their feet."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d543d35-945a-4ffa-beb7-7c5d4f894f79.jpg?1764121553"
    }
}
