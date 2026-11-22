package com.wingedsheep.mtg.sets.definitions.trc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Christine Chapel, Combat Medic
 * {3}{G}
 * Legendary Creature — Human Doctor
 * 2/2
 *
 * Strange new worlds — When Christine Chapel enters, you gain life
 * equal to the number of differently named lands you control.
 *
 * Whenever you gain life, put two +1/+1 counters on Christine Chapel.
 */
val ChristineChapelCombatMedic = card("Christine Chapel, Combat Medic") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Doctor"
    oracleText = "Strange new worlds — When Christine Chapel enters, you gain life " +
            "equal to the number of differently named lands you control.\n" +
            "Whenever you gain life, put two +1/+1 counters on Christine Chapel."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(DynamicAmounts.differentlyNamedLandsYouControl())
    }

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Pauline Voss"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2efcab5-3aad-4edd-87d1-289978e8855e.jpg?1786395991"
    }
}
