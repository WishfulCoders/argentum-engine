package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thousand Moons Crackshot
 * {1}{W}
 * Creature — Human Soldier
 * 2/2
 *
 * Whenever this creature attacks, you may pay {2}{W}. When you do, tap target creature.
 *
 * The attack trigger is a "When you do" reflexive: the optional {2}{W} payment is the action,
 * and the reflexive ability — which targets a creature, chosen as it goes on the stack — only
 * fires if the payment is made.
 */
val ThousandMoonsCrackshot = card("Thousand Moons Crackshot") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature attacks, you may pay {2}{W}. When you do, tap target creature."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReflexiveTrigger(
            // "you may pay {2}{W}"
            action = Effects.PayMana("{2}{W}"),
            optional = true) {
            // "When you do, tap target creature."
            val creature = target(TargetFilter.Creature)
            effect = Effects.Tap(creature)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Marie Magny"
        flavorText = "The Thousand Moons practice every maneuver thousands of times, allowing them " +
            "to strike flawlessly without hesitation in the heat of battle."
        imageUri = "https://cards.scryfall.io/normal/front/7/4/741a7439-965d-49f2-b43e-053f29196e6b.jpg?1782694581"
    }
}
