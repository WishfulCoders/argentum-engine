package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Systems Override
 * {2}{R}
 * Sorcery
 * Gain control of target artifact or creature until end of turn. Untap that permanent. It gains haste until end of turn. If it's a Spacecraft, put ten charge counters on it. If you do, remove ten charge counters from it at the beginning of the next end step.
 */
val SystemsOverride = card("Systems Override") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target artifact or creature until end of turn. Untap that permanent. It gains haste until end of turn. If it's a Spacecraft, put ten charge counters on it. If you do, remove ten charge counters from it at the beginning of the next end step."

    spell {
        val target = target(TargetFilter.CreatureOrArtifact)
        effect = Effects.GainControl(target, Duration.EndOfTurn) then
            Effects.Untap(target) then
            Effects.GrantKeyword(Keyword.HASTE, target) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withSubtype("Spacecraft"), target),
                then = Effects.AddCounters(CounterType.CHARGE, 10, target) then
                    Effects.CreateDelayedTrigger(
                        step = Step.END,
                        effect = Effects.RemoveCounters(CounterType.CHARGE, 10, target)
                    )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Hardy Fowler"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a34c71b-8d3c-435b-9cf8-4902f997d10d.jpg?1752947203"
    }
}
