package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity

/**
 * Take Out the Trash
 * {1}{R}
 * Instant
 * Take Out the Trash deals 3 damage to target creature or planeswalker.
 * If you control a Raccoon, you may discard a card. If you do, draw a card.
 */
val TakeOutTheTrash = card("Take Out the Trash") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Take Out the Trash deals 3 damage to target creature or planeswalker. If you control a Raccoon, you may discard a card. If you do, draw a card."

    spell {
        val creatureOrPw = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(3, creatureOrPw) then
            Effects.If(
                condition = Conditions.ControlCreatureOfType(Subtype("Raccoon")),
                then = Effects.May(
                    Patterns.Hand.discardCards(1) then Effects.DrawCards(1)
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Fiona Hsieh"
        flavorText = "Disgusting. Revolting. Effective."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a1c6f00-af4c-4d35-b682-6c0e759df9a5.jpg?1721426722"
    }
}
