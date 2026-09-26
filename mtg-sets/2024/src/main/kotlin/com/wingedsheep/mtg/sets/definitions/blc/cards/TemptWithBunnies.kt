package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

private const val offerText =
    "Tempting Offer — Draw a card and create a 1/1 white Rabbit creature token. " +
        "Then each opponent may draw a card and create a 1/1 white Rabbit creature token. " +
        "For each opponent who does, you draw a card and you create a 1/1 white Rabbit creature token."

/**
 * Tempt with Bunnies
 * {2}{W}
 * Sorcery
 * Tempting Offer — Draw a card and create a 1/1 white Rabbit creature token. Then each opponent
 * may draw a card and create a 1/1 white Rabbit creature token. For each opponent who does, you
 * draw a card and you create a 1/1 white Rabbit creature token.
 *
 * [Patterns.Mechanic.temptingOffer] runs the offer for you, collects every opponent's yes/no in
 * turn order, then runs it for each accepting opponent and once more for you per acceptance.
 */
val TemptWithBunnies = card("Tempt with Bunnies") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = offerText

    spell {
        effect = Patterns.Mechanic.temptingOffer(
            offer = Effects.DrawCards(1) then
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Rabbit"),
                    imageUri = "https://cards.scryfall.io/normal/front/8/1/81de52ef-7515-4958-abea-fb8ebdcef93c.jpg?1783909772"
                ),
            description = offerText,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Ben Wootten"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90cc0369-dc9b-4e75-8e3c-ec0783fa13a9.jpg?1783910734"
        ruling("2024-07-26", "Your opponents decide in turn order whether or not they accept the offer, starting with the next opponent in turn order. Each opponent will know the decisions of previous opponents in turn order when making their decision.")
        ruling("2024-07-26", "After each opponent has decided, the effect happens simultaneously for each one who accepted the offer. Then the effect happens again for you a number of times equal to the number of opponents who accepted.")
    }
}
