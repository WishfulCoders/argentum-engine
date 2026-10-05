package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Currency Converter — New Capenna Commander #81
 * {1} · Artifact · Rare
 *
 * Whenever you discard a card, you may exile that card from your graveyard.
 * {2}, {T}: Draw a card, then discard a card.
 * {T}: Put a card exiled with this artifact into its owner's graveyard. If it's a land card, create
 * a Treasure token. If it's a nonland card, create a 2/2 black Rogue creature token.
 *
 * Modelling notes:
 * - The discard trigger is Moonstone, Harsh Mistress's "you may exile that card" pipeline, with the
 *   exile linked to this artifact so the {T} ability can find the card later. "From your graveyard"
 *   is honoured: a discarded card that didn't end up there (madness) is not exiled.
 * - The {T} ability chooses one card from the linked pile and moves it to the graveyard; the land /
 *   nonland check reads the moved card. An empty pile still lets you activate it, and nothing
 *   happens. The pile only ever holds cards you discarded (cards in a hand are always that hand's
 *   owner's), so the graveyard it goes to is its owner's.
 */
val CurrencyConverter = card("Currency Converter") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you discard a card, you may exile that card from your graveyard.\n" +
        "{2}, {T}: Draw a card, then discard a card.\n" +
        "{T}: Put a card exiled with this artifact into its owner's graveyard. If it's a land card, " +
        "create a Treasure token. If it's a nonland card, create a 2/2 black Rogue creature token."

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.May(
            Effects.Pipeline {
                val discarded = gather(CardSource.TriggeringEntity)
                val inGraveyard = filter(discarded, GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD))
                exile(inGraveyard, linkToSource = true)
            },
            descriptionOverride = "Exile the discarded card from your graveyard?"
        )
        description = "Whenever you discard a card, you may exile that card from your graveyard."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.DrawCards(1) then Patterns.Hand.discardCards(1)
        description = "{2}, {T}: Draw a card, then discard a card."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            val pile = gather(CardSource.FromLinkedExile())
            val chosen = chooseExactly(1, pile, prompt = "Choose a card exiled with Currency Converter")
            toGraveyard(chosen)
            ifNotEmpty(chosen, filter = GameObjectFilter.Land) {
                run(Effects.CreateTreasure())
            } orElse {
                ifNotEmpty(chosen) {
                    run(
                        Effects.CreateToken(
                            power = 2,
                            toughness = 2,
                            colors = setOf(Color.BLACK),
                            creatureTypes = setOf("Rogue"),
                            imageUri = "https://cards.scryfall.io/normal/front/0/5/0526330e-40eb-4c5f-b0f5-5489839e5f44.jpg?1789737431"
                        )
                    )
                }
            }
        }
        description = "{T}: Put a card exiled with this artifact into its owner's graveyard. If it's a " +
            "land card, create a Treasure token. If it's a nonland card, create a 2/2 black Rogue " +
            "creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Sean Murray"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/187b6719-e5ed-4615-a00b-3313ceca055b.jpg?1783923344"
    }
}
