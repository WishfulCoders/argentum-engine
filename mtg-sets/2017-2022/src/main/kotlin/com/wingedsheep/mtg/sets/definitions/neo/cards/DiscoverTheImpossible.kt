package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Discover the Impossible — Kamigawa: Neon Dynasty #50
 * {2}{U} · Instant · Uncommon
 *
 * Look at the top five cards of your library. Exile one of them face down and put the rest on the
 * bottom of your library in a random order. You may cast the exiled card without paying its mana
 * cost if it's an instant spell with mana value 2 or less. If you don't, put that card into your hand.
 *
 * Hideaway's look-and-exile-face-down shape ([FaceDownMode.HIDDEN]), followed by Beseech the
 * Mirror's "cast it for free, else to hand" tail. The free cast goes through `chooseSpell`, which
 * tests the instant / mana-value-2 filter against the face that would actually be cast — per the
 * ruling, an adventurer, split card or modal DFC qualifies when the *resulting spell* is an instant
 * with mana value 2 or less. Declining (or having no such face) leaves the card in exile, and
 * whatever is still in exile goes to hand.
 */
val DiscoverTheImpossible = card("Discover the Impossible") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at the top five cards of your library. Exile one of them face down and put " +
        "the rest on the bottom of your library in a random order. You may cast the exiled card " +
        "without paying its mana cost if it's an instant spell with mana value 2 or less. If you " +
        "don't, put that card into your hand."

    spell {
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(count = 5, player = Player.You))
            val (picked, rest) = chooseExactlySplit(
                1,
                from = top,
                prompt = "Choose a card to exile face down",
                selectedLabel = "Exile face down",
                remainderLabel = "Put on bottom of library"
            )
            val exiled = moveTracked(
                picked,
                CardDestination.ToZone(Zone.EXILE),
                faceDown = FaceDownMode.HIDDEN
            )
            toLibraryBottom(rest, order = CardOrder.Random)
            val castable = chooseSpell(
                from = exiled,
                filter = GameObjectFilter.Instant.manaValueAtMost(2),
                showAllCards = true,
                prompt = "You may cast the exiled card without paying its mana cost if it's an " +
                    "instant spell with mana value 2 or less",
                selectedLabel = "Cast for free"
            )
            run(Effects.CastFromCollectionWithoutPayingCost(castable))
            // "If you don't, put that card into your hand." — the cast card has left exile.
            toHand(filter(exiled, GameObjectFilter.Any.currentlyIn(Zone.EXILE)))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c201117-9850-4e92-9ecc-e4d71a70a959.jpg?1783923908"
        ruling(
            "2022-02-18",
            "You may cast the card you exile with Discover the Impossible as long as the resulting " +
                "spell is an instant spell with mana value 2 or less, even if the card you exiled " +
                "doesn't match that criteria. This may happen if you exile a split card, a modal " +
                "double-faced card, an adventurer card, or similar."
        )
    }
}
