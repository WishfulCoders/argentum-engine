package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Contract from Below
 * {B}
 * Sorcery
 * Remove this card from your deck before playing if you're not playing for ante.
 * Discard your hand, ante the top card of your library, then draw seven cards.
 *
 * The first line is deck construction, not an ability: the server's deck validation only lets
 * this card into a game played for ante (CR 407.3). Anteing is the ordinary library-top move into
 * the ante zone ([Patterns.Library.anteTop]).
 */
val ContractFromBelow = card("Contract from Below") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Remove this card from your deck before playing if you're not playing for ante.\n" +
        "Discard your hand, ante the top card of your library, then draw seven cards."

    spell {
        effect = Patterns.Hand.discardHand() then
            Patterns.Library.anteTop() then
            Effects.DrawCards(7)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9853b0ce-4763-4877-9741-f9145a3659c6.jpg?1783948698"
    }
}
