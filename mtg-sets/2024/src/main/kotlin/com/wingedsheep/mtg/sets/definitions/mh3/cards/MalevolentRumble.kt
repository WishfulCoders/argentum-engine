package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Malevolent Rumble
 * {1}{G}
 * Sorcery
 *
 * Reveal the top four cards of your library. You may put a permanent card from among them into
 * your hand. Put the rest into your graveyard. Create a 0/1 colorless Eldrazi Spawn creature
 * token with "Sacrifice this token: Add {C}."
 *
 * Modeling notes:
 *  - Unlike Cache Grab (which mills first, then moves a chosen card back out of the graveyard),
 *    Malevolent Rumble only *reveals* — the card put into hand never touches the graveyard. So
 *    this is Gather(revealed) → Select(up to one permanent, storeRemainder) → Move(selected →
 *    hand) → Move(remainder → graveyard), not Cache Grab's mill-then-recall shape.
 *  - `Effects.CreateEldraziSpawn()` mints the token (see `PredefinedTokens.EldraziSpawn`).
 */
val MalevolentRumble = card("Malevolent Rumble") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top four cards of your library. You may put a permanent card from " +
        "among them into your hand. Put the rest into your graveyard. Create a 0/1 colorless " +
        "Eldrazi Spawn creature token with \"Sacrifice this token: Add {C}.\""

    spell {
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(4), revealed = true)
            val (toHandCards, toGraveyardCards) = chooseUpToSplit(
                1,
                from = revealed,
                filter = GameObjectFilter.Permanent,
                showAllCards = true,
                prompt = "You may put a permanent card into your hand",
                selectedLabel = "Put into hand",
                remainderLabel = "Put into graveyard"
            )
            toHand(toHandCards)
            toGraveyard(toGraveyardCards)
            run(Effects.CreateEldraziSpawn())
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a178cfe8-f9fa-4255-88d0-54a0bed079f5.jpg?1783911259"
    }
}
