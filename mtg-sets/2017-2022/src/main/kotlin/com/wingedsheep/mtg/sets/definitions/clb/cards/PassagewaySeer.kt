package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Passageway Seer
 * {3}{B}
 * Creature — Tiefling Warlock
 * 2/2
 * Lifelink
 * When this creature enters, you take the initiative.
 * At the beginning of your end step, if you have the initiative, put a +1/+1 counter on this creature.
 */
val PassagewaySeer = card("Passageway Seer") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Tiefling Warlock"
    power = 2
    toughness = 2
    oracleText = "Lifelink\nWhen this creature enters, you take the initiative.\n" +
        "At the beginning of your end step, if you have the initiative, put a +1/+1 counter on this creature."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.TakeTheInitiative()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouHaveTheInitiative
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Konstantin Porubov"
        flavorText = "\"I'm certain there's a shortcut here . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8aeee54a-5e16-4519-b04a-49eaaf896b9a.jpg?1783922757"
    }
}
