package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Pulsar Squadron Ace
 * {1}{W}
 * Creature — Human Pilot
 * When this creature enters, look at the top five cards of your library. You may reveal a Spacecraft card from among them and put it into your hand. Put the rest on the bottom of your library in a random order. If you didn't put a card into your hand this way, put a +1/+1 counter on this creature.
 * 1/2
 */
val PulsarSquadronAce = card("Pulsar Squadron Ace") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Pilot"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, look at the top five cards of your library. You may reveal a Spacecraft card from among them and put it into your hand. Put the rest on the bottom of your library in a random order. If you didn't put a card into your hand this way, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            // 1. Look at the top five cards of your library
            val looked = gather(CardSource.TopOfLibrary(5))
            // 2. You may reveal a Spacecraft card from among them and put it into your hand
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.HasSubtype(com.wingedsheep.sdk.core.Subtype("Spacecraft"))
                    )
                ),
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            // 3. Put the rest on the bottom of your library in a random order
            toLibraryBottom(rest, order = CardOrder.Preserve)
            // 4. If you didn't put a card into your hand this way, put a +1/+1 counter on this creature
            // If cards were kept, do nothing
            ifNotEmpty(kept) {
                run(Effects.Nothing)
            } orElse {
                run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
                // If no cards kept, add counter
            }
            toHand(kept, revealed = true)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d989cdc-cbd7-4b71-9589-59618597ac8a.jpg?1752946664"
    }
}
