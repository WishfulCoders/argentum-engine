package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Succumb to the Cold
 * {2}{U}
 * Instant
 *
 * Tap one or two target creatures an opponent controls. Put a stun counter on each of them.
 * (If a permanent with a stun counter would become untapped, remove one from it instead.)
 *
 * "One or two target" is `TargetCreature(count = 2, minCount = 1)`; the tap-and-stun rider runs
 * per chosen target via [ForEachTargetEffect], so a target that has become illegal by resolution
 * is simply skipped rather than fizzling the whole spell.
 */
val SuccumbToTheCold = card("Succumb to the Cold") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap one or two target creatures an opponent controls. Put a stun counter on " +
        "each of them. (If a permanent with a stun counter would become untapped, remove one " +
        "from it instead.)"

    spell {
        target = TargetObject(filter = TargetFilter.Creature.opponentControls(), count = 2, minCount = 1)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.AddCounters(
                counterType = CounterType.STUN,
                count = 1,
                target = EffectTarget.ContextTarget(0)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Andrew Mar"
        flavorText = "\"I've never known cold like it. It burned like fire and bit like an adder's " +
            "fangs. Call me a coward if you wish, but I won't go back there.\"\n" +
            "—Syr Lydel, knight of Vantress"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c14d9fc0-bfbf-4359-93bf-5e53466965d6.jpg?1783915114"
    }
}
