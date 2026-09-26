package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Errand-Rider of Gondor
 * {2}{W}
 * Creature — Human Soldier
 * 3/2
 *
 * When this creature enters, draw a card. Then if you don't control a legendary
 * creature, put a card from your hand on the bottom of your library.
 */
val ErrandRiderOfGondor = card("Errand-Rider of Gondor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, draw a card. Then if you don't control a legendary creature, put a card from your hand on the bottom of your library."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1) then
            Effects.If(
                condition = Conditions.Not(
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.legendary())
                ),
                then = Effects.Pipeline {
                    val handCards = gather(CardSource.FromZone(Zone.HAND, Player.You))
                    val chosen = chooseExactly(
                        1,
                        from = handCards,
                        prompt = "Put a card from your hand on the bottom of your library"
                    )
                    toLibraryBottom(chosen, order = CardOrder.Preserve)
                }
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "YW Tang"
        flavorText = "\"Gondor is in great need. Lord Denethor asks for all your strength and all your speed, lest Gondor fall at last.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3f990e7-54a3-4893-8510-645b2065447b.jpg?1686967733"
    }
}
