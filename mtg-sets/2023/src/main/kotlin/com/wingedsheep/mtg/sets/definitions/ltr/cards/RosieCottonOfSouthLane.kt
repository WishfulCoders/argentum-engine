package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rosie Cotton of South Lane
 * {2}{W}
 * Legendary Creature — Halfling Peasant
 * 1/1
 *
 * When Rosie Cotton enters, create a Food token. (It's an artifact with "{2}, {T},
 * Sacrifice this token: You gain 3 life.")
 * Whenever you create a token, put a +1/+1 counter on target creature you control other
 * than Rosie Cotton.
 */
val RosieCottonOfSouthLane = card("Rosie Cotton of South Lane") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Halfling Peasant"
    power = 1
    toughness = 1
    oracleText = "When Rosie Cotton enters, create a Food token. (It's an artifact with \"{2}, {T}, " +
        "Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever you create a token, put a +1/+1 counter on target creature you control other " +
        "than Rosie Cotton."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.you.createsToken()
        val otherCreatureYouControl = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, otherCreatureYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Claudiu-Antoniu Magherusan"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75338f49-1f02-4333-87e4-5779ef14e688.jpg?1686967894"
    }
}
