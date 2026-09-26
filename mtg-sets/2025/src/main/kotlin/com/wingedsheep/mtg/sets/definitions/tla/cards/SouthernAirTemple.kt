package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Southern Air Temple
 * {3}{W}
 * Legendary Enchantment — Shrine
 * When Southern Air Temple enters, put X +1/+1 counters on each creature you control, where X is
 * the number of Shrines you control.
 * Whenever another Shrine you control enters, put a +1/+1 counter on each creature you control.
 *
 * The Shrine cycle (Honden-style): the ETB counts Shrines you control including itself, then each
 * subsequent Shrine that enters pumps the team again.
 */
val SouthernAirTemple = card("Southern Air Temple") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "When Southern Air Temple enters, put X +1/+1 counters on each creature you control, " +
        "where X is the number of Shrines you control.\n" +
        "Whenever another Shrine you control enters, put a +1/+1 counter on each creature you control."

    spell {}

    // When Southern Air Temple enters, put X +1/+1 counters on each creature you control,
    // where X is the number of Shrines you control (counting itself).
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddDynamicCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Permanent.withSubtype(Subtype("Shrine"))
                ).count(),
                target = EffectTarget.IterationEntity
            )
        )
    }

    // Whenever another Shrine you control enters, put a +1/+1 counter on each creature you control.
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype(Subtype("Shrine")).youControl()).enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                count = 1,
                target = EffectTarget.IterationEntity
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Salvatorre Zee Yazzie"
        flavorText = "The birthplace of the last Airbender."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c172f9b-2184-4736-9f90-74ce08596292.jpg?1764120133"
    }
}
