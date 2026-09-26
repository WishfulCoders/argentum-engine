package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Luminarch Aspirant
 * {1}{W}
 * Creature — Human Cleric
 * 1/1
 *
 * At the beginning of combat on your turn, put a +1/+1 counter on target creature you control.
 */
val LuminarchAspirant = card("Luminarch Aspirant") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "At the beginning of combat on your turn, put a +1/+1 counter on target " +
        "creature you control."

    triggeredAbility {
        val creatureYouControl = target(TargetFilter.CreatureYouControl)
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creatureYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Mads Ahm"
        flavorText = "\"Rally to my light, and together we will drive out this darkness!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe964e7e-e2c5-4263-889d-0a531eb51442.jpg"
    }
}
