package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

val SomethingWorthSaving = card("Something Worth Saving") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Mill four cards. You may put a permanent card from among them into your hand. You gain 1 life. " +
        "(To mill four cards, put the top four cards of your library into your graveyard.)"

    spell {
        effect = Effects.Pipeline {
            val milled = mill(4)
            val selected = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Permanent,
                showAllCards = true,
                prompt = "You may put a permanent card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(selected)
            run(Effects.GainLife(1))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "PINDURSKI"
        flavorText = "\"Jace cut out all the best pieces of himself. If we find them, perhaps they can help save him.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02ee7817-40af-4fcf-a2df-eb218b669281.jpg?1788878199"
        inBooster = false
    }
}
