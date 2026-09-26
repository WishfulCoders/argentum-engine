package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nimble Hobbit
 * {1}{W}
 * Creature — Halfling Peasant
 * 1/3
 *
 * Whenever this creature attacks, you may sacrifice a Food or pay {2}{W}.
 * When you do, tap target creature an opponent controls.
 */
val NimbleHobbit = card("Nimble Hobbit") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Halfling Peasant"
    power = 1
    toughness = 3
    oracleText = "Whenever this creature attacks, you may sacrifice a Food or pay {2}{W}. When you do, tap target creature an opponent controls."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReflexiveTrigger(
            // "you may sacrifice a Food or pay {2}{W}"
            action = Effects.ChooseAction(
                choices = listOf(
                    EffectChoice(
                        label = "Sacrifice a Food",
                        effect = Effects.SacrificeOwn(
                            filter = GameObjectFilter.Any.withSubtype("Food")
                        ),
                        feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                            filter = GameObjectFilter.Any.withSubtype("Food")
                        )
                    ),
                    EffectChoice(
                        label = "Pay {2}{W}",
                        effect = Effects.PayMana("{2}{W}")
                    )
                )
            ),
            optional = true) {
            // "When you do, tap target creature an opponent controls."
            val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls)
            effect = Effects.Tap(creatureOpponentControls)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "JB Casacop"
        flavorText = "The ruffian knew too little of Hobbits to understand his peril. Foolishly, he decided to fight."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4cd0756-7bf3-4cb6-9687-1f9346b0bb92.jpg?1686967856"
    }
}
