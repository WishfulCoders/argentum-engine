package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects

/**
 * Minas Tirith Garrison
 * {3}{U}
 * Creature — Human Soldier
 * * / 5
 *
 * Minas Tirith Garrison's power is equal to the number of cards in your hand.
 * Whenever this creature attacks, you may tap any number of untapped Humans you control.
 * Draw a card for each Human tapped this way.
 */
val MinasTirithGarrison = card("Minas Tirith Garrison") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    oracleText = "Minas Tirith Garrison's power is equal to the number of cards in your hand.\n" +
        "Whenever this creature attacks, you may tap any number of untapped Humans you control. Draw a card for each Human tapped this way."

    // Power is equal to the number of cards in your hand.
    dynamicPower(
        DynamicAmounts.cardsInYourHand()
    )

    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val humans = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.youControl().withSubtype("Human").untapped()
                )
            )
            val tapped = chooseAnyNumber(
                from = humans,
                prompt = "Tap any number of untapped Humans you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(tapped, tap = true))
            run(Effects.DrawCards(tapped.count))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "825"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9023a7eb-fe74-4e3d-9279-e69519b41fbd.jpg?1719684213"
    }
}
