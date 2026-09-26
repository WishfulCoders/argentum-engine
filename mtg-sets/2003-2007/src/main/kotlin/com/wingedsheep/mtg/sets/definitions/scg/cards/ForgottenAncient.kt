package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Forgotten Ancient
 * {3}{G}
 * Creature — Elemental
 * 0/3
 * Whenever a player casts a spell, you may put a +1/+1 counter on Forgotten Ancient.
 * At the beginning of your upkeep, you may move any number of +1/+1 counters from
 * Forgotten Ancient onto other creatures.
 */
val ForgottenAncient = card("Forgotten Ancient") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 0
    toughness = 3
    oracleText = "Whenever a player casts a spell, you may put a +1/+1 counter on Forgotten Ancient.\nAt the beginning of your upkeep, you may move any number of +1/+1 counters from Forgotten Ancient onto other creatures."

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts()
        effect = Effects.May(
            Effects.AddCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                count = 1,
                target = EffectTarget.Self
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(Effects.DistributeCountersFromSelf(CounterType.PLUS_ONE_PLUS_ONE))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Mark Tedin"
        flavorText = "Its blood is life. Its body is growth."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49d3b91d-2e4f-4574-89f8-7b804f1d21bf.jpg?1562528527"
        ruling("2022-12-08", "Forgotten Ancient's first ability will resolve before the spell that caused it to trigger. Putting a +1/+1 counter on Forgotten Ancient is optional.")
        ruling("2022-12-08", "Forgotten Ancient's last ability doesn't target any creatures. You choose how many +1/+1 counters will be moved (and onto which creatures) as the ability resolves.")
    }
}
