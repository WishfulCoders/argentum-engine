package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angelic Quartermaster
 * {3}{W}{W}
 * Creature — Angel Soldier
 * 3/3
 *
 * Flying
 * When this creature enters, put a +1/+1 counter on each of up to two other target creatures.
 *
 * The ETB fans a 0–2 optional [TargetCreature] filtered to [TargetFilter.OtherCreature] (any
 * controller, excludes itself) out with [ForEachTargetEffect] so each chosen creature receives
 * one +1/+1 counter — the same shape as Felidar Savior, but with the wider "other creatures"
 * filter (not restricted to creatures you control).
 */
val AngelicQuartermaster = card("Angelic Quartermaster") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Soldier"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, put a +1/+1 counter on each of up to two other target creatures."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherCreature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
        description = "When this creature enters, put a +1/+1 counter on each of up to two other " +
            "target creatures."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "PINDURSKI"
        flavorText = "\"Stand strong. We will reclaim the dawn.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41d81b88-c19b-4148-89ba-ae8fb53843e1.jpg?1782703194"
    }
}
