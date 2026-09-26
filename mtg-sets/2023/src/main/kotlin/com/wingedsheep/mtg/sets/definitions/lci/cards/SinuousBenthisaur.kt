package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sinuous Benthisaur
 * {5}{U}
 * Creature — Dinosaur
 * 4/4
 *
 * When this creature enters, look at the top X cards of your library, where X is the number of
 * Caves you control plus the number of Cave cards in your graveyard. Put two of those cards into
 * your hand and the rest on the bottom of your library in a random order.
 *
 * X (evaluated at resolution, same pattern as Calamitous Cave-In / Gargantuan Leech):
 *   - Battlefield: permanents with the Cave land subtype you control
 *     ([DynamicAmount.Count] over [Zone.BATTLEFIELD], [GameObjectFilter.Land.withSubtype]("Cave"))
 *   - Graveyard:   any card with the Cave subtype in your graveyard
 *     ([DynamicAmount.Count] over [Zone.GRAVEYARD], [GameObjectFilter.Any.withSubtype]("Cave"))
 *   summed via [DynamicAmount.Add].
 *
 * Dig pipeline (private look — no reveal — then keep two, rest to bottom in random order):
 *   1. GatherCards(TopOfLibrary(X)) → "looked"         — pulled off the library so the controller
 *                                                         sees them privately during selection.
 *   2. SelectFromCollection("looked", ChooseExactly(2)) — the mandatory "put two into your hand".
 *        storeSelected = "kept", storeRemainder = "rest". ChooseExactly caps at the collection
 *        size, so with X < 2 the controller simply keeps everything looked at.
 *   3. MoveCollection("kept" → HAND) — not revealed; the whole look is private.
 *   4. MoveCollection("rest" → LIBRARY Bottom, order = CardOrder.Random) — "on the bottom of your
 *        library in a random order"; CardOrder.Random shuffles the moved cards and strips their
 *        reveal markers so the controller has no knowledge of their order.
 *
 * LCI #76, John Tedrick.
 */
val SinuousBenthisaur = card("Sinuous Benthisaur") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dinosaur"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, look at the top X cards of your library, where X is " +
        "the number of Caves you control plus the number of Cave cards in your graveyard. Put two " +
        "of those cards into your hand and the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.self.enters()

        val cavesControlled = DynamicAmounts.count(
            Player.You,
            Zone.BATTLEFIELD,
            GameObjectFilter.Land.withSubtype("Cave"),
        )
        val cavesInGraveyard = DynamicAmounts.count(
            Player.You,
            Zone.GRAVEYARD,
            GameObjectFilter.Any.withSubtype("Cave"),
        )

        effect = Effects.Pipeline {
            val looked = gather(
                CardSource.TopOfLibrary(
                    cavesControlled + cavesInGraveyard
                )
            )
            val (kept, rest) = chooseExactlySplit(
                2,
                from = looked,
                selectedLabel = "Put into hand",
                remainderLabel = "Put on bottom",
                prompt = "Put two of those cards into your hand"
            )
            toHand(kept)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "John Tedrick"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a58639c-001f-4cb1-89fd-0a0967b86977.jpg?1782694549"
    }
}
