package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kujar Seedsculptor
 * {1}{G}
 * Creature — Elf Druid
 * 1/2
 * When this creature enters, put a +1/+1 counter on target creature you control.
 *
 * The trigger's target is a plain "creature you control" — Kujar Seedsculptor is already on the
 * battlefield when the ability goes on the stack, so it is itself a legal target (per the KLD
 * ruling); no `.other()` narrowing.
 */
val KujarSeedsculptor = card("Kujar Seedsculptor") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, put a +1/+1 counter on target creature you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Anna Steinbauer"
        flavorText = "Every leaf, tree, and building in Kujar has been placed to achieve maximum harmony, in accordance with the elvish philosophy known as the Great Conduit."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8664c65-77ca-4881-959a-e1923e1a6b98.jpg?1783937178"

        ruling(
            "2016-09-20",
            "Kujar Seedsculptor can be the target of its own triggered ability."
        )
    }
}
