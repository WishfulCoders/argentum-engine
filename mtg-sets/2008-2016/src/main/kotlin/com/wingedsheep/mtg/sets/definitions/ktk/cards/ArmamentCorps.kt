package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Armament Corps
 * {2}{W}{B}{G}
 * Creature — Human Soldier
 * 4/4
 * When Armament Corps enters the battlefield, distribute two +1/+1 counters
 * among one or two target creatures you control.
 */
val ArmamentCorps = card("Armament Corps") {
    manaCost = "{2}{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, distribute two +1/+1 counters among one or two target creatures you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target = TargetObject(filter = TargetFilter.CreatureYouControl, count = 2, minCount = 1)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Steven Belledin"
        flavorText = "The Abzan avoid extended supply lines by incorporating weapons stores into their battle formations."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5c69876-809d-4af3-9fd6-3bac41541dad.jpg?1562791520"
    }
}
