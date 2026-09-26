package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Pictures of Spider-Man
 * {2}{G}
 * Artifact
 *
 * When this artifact enters, look at the top five cards of your library. You may reveal up to two
 * creature cards from among them and put them into your hand. Put the rest on the bottom of your
 * library in a random order.
 * {1}, {T}, Sacrifice this artifact: Create a Treasure token. (It's an artifact with "{T},
 * Sacrifice this token: Add one mana of any color.")
 *
 * Implementation:
 *  - ETB trigger inlines the shared look-at-top reveal pipeline
 *    ([com.wingedsheep.sdk.dsl.Patterns.Library.lookAtTopRevealMatchingToHand] shape) so the keep
 *    count can be two: gather the top five, an optional [SelectFromCollectionEffect.ChooseUpTo]`(2)`
 *    filtered to [GameObjectFilter.Creature] revealed to hand, then the remainder to the bottom of
 *    the library in a random order.
 *  - Activated ability cost = {1} + [Costs.Tap] + [Costs.SacrificeSelf]; effect = [Effects.CreateTreasure].
 */
val PicturesOfSpiderMan = card("Pictures of Spider-Man") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, look at the top five cards of your library. You may " +
        "reveal up to two creature cards from among them and put them into your hand. Put the rest " +
        "on the bottom of your library in a random order.\n" +
        "{1}, {T}, Sacrifice this artifact: Create a Treasure token. (It's an artifact with \"{T}, " +
        "Sacrifice this token: Add one mana of any color.\")"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(5))
            // You may reveal up to two creature cards from among them and put them into your hand.
            val (kept, rest) = chooseUpToSplit(
                2,
                from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "You may reveal up to two creature cards to put into your hand.",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            // Put the rest on the bottom of your library in a random order.
            toLibraryBottom(rest, order = CardOrder.Random)
        }
        description = "When this artifact enters, look at the top five cards of your library. You " +
            "may reveal up to two creature cards from among them and put them into your hand. Put " +
            "the rest on the bottom of your library in a random order."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.CreateTreasure()
        description = "{1}, {T}, Sacrifice this artifact: Create a Treasure token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Rafater"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1ec41d4-0180-42f7-9c54-f3c39b4ffb8d.jpg?1783905326"
    }
}
