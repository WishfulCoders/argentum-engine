package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Manamorphose
 * {1}{R/G}
 * Instant
 * Add two mana in any combination of colors.
 * Draw a card.
 *
 * [Effects.AddManaInAnyCombination] (the Smokebraider effect, unrestricted) followed by a draw; the
 * colors are chosen before the card is drawn (ruling 2020-08-07). It is an instant that adds mana,
 * not a mana ability, so it uses the stack.
 */
val Manamorphose = card("Manamorphose") {
    manaCost = "{1}{R/G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Add two mana in any combination of colors.\nDraw a card."

    spell {
        effect = Effects.AddManaInAnyCombination(2) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "211"
        artist = "Jeff Miracola"
        flavorText = "For a moment, objects of pure mana glimmered in the wonderstruck boggart's hands. In the " +
            "next moment, they were in his mouth, as he chewed contentedly."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50283122-b8c4-4fb3-8eba-6252b72222f4.jpg?1783942721"
        ruling("2020-08-07", "You choose which color or colors of mana to add before you draw a card.")
    }
}
