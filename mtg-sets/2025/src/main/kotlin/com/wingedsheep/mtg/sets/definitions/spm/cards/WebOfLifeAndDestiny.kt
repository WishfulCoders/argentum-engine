package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.core.Step

/**
 * Web of Life and Destiny
 * {6}{G}{G}
 * Enchantment
 *
 * Convoke (Your creatures can help cast this spell. Each creature you tap while casting this
 * spell pays for {1} or one mana of that creature's color.)
 * At the beginning of combat on your turn, look at the top five cards of your library. You may
 * put a creature card from among them onto the battlefield. Put the rest on the bottom of your
 * library in a random order.
 *
 * Implementation:
 *  - [Keyword.CONVOKE] is a cost-reduction keyword handled by the engine's alternative-payment path.
 *  - The begin-combat trigger inlines the shared look-at-top dig pipeline: gather the top five
 *    (looked at, not revealed), an optional [SelectFromCollectionEffect.ChooseUpTo]`(1)` filtered to
 *    [GameObjectFilter.Creature] moved onto the battlefield, then the remainder to the bottom of the
 *    library in a random order. Unlike Pictures of Spider-Man (which puts creatures into hand), the
 *    kept creature goes straight onto the battlefield.
 */
val WebOfLifeAndDestiny = card("Web of Life and Destiny") {
    manaCost = "{6}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "At the beginning of combat on your turn, look at the top five cards of your library. You " +
        "may put a creature card from among them onto the battlefield. Put the rest on the bottom " +
        "of your library in a random order."

    keywords(Keyword.CONVOKE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.Pipeline {
            // Look at the top five cards of your library.
            val looked = gather(CardSource.TopOfLibrary(5))
            // You may put a creature card from among them onto the battlefield.
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "You may put a creature card onto the battlefield.",
                showAllCards = true
            )
            move(kept, CardDestination.ToZone(Zone.BATTLEFIELD))
            // Put the rest on the bottom of your library in a random order.
            toLibraryBottom(rest, order = CardOrder.Random)
        }
        description = "At the beginning of combat on your turn, look at the top five cards of your " +
            "library. You may put a creature card from among them onto the battlefield. Put the " +
            "rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "122"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b3c609e-f7c9-4fe5-84d9-4f4c76020a4b.jpg?1783905321"
    }
}
