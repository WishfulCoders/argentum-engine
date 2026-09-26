package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

val WildOnslaught = card("Wild Onslaught") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Kicker {4} (You may pay an additional {4} as you cast this spell.)\nPut a +1/+1 counter on each creature you control. If this spell was kicked, put two +1/+1 counters on each creature you control instead."

    keywordAbility(KeywordAbility.kicker("{4}"))

    spell {
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesYouControl,
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.IterationEntity)
            ),
            otherwise = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesYouControl,
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb75cf21-08be-4b92-bdf6-014a36090738.jpg?1562744928"
    }
}
