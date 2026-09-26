package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Lightning Coils — Mirrodin #198. */
val LightningCoils = card("Lightning Coils") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever a nontoken creature you control dies, put a charge counter on this " +
        "artifact.\nAt the beginning of your upkeep, if this artifact has five or more charge " +
        "counters on it, remove all of them from it and create that many 3/1 red Elemental creature " +
        "tokens with haste. Exile them at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().nontoken()).dies()
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 5)
        effect = Effects.Pipeline {
            val removedChargeCounters = storeNumber(DynamicAmounts.countersOnSelf(CounterType.CHARGE))
            run(Effects.RemoveAllCountersOfType(CounterType.CHARGE, EffectTarget.Self))
            run(Effects.CreateToken(
                count = removedChargeCounters.amount,
                power = 3,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Elemental"),
                keywords = setOf(Keyword.HASTE),
                exileAtStep = Step.END,
                imageUri = "https://cards.scryfall.io/normal/front/e/4/e4a9051b-f964-43f9-877b-ea4f17620ecb.jpg?1783915251",
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a800f326-3416-4917-a1cf-0f90255777d3.jpg?1783944514"
    }
}
