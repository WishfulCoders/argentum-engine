package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Moonlight Bargain — Ravnica: City of Guilds #95
 * {3}{B}{B} · Instant · Rare
 *
 * Look at the top five cards of your library. For each card, put that card into your graveyard
 * unless you pay 2 life. Then put the rest into your hand.
 *
 * The look is a top-of-library gather (a private look, so the five are revealed to you and the
 * client can show each one as it comes up), and "for each card … unless you pay 2 life" is one
 * [Effects.MayPay] per card inside a [ForEachInCollectionEffect]: `PayLifeEffect(2)` is the
 * cost, so a player at 1 life cannot pay (CR 119.4) and the card goes to the graveyard; the
 * prompt carries the card being decided as its subject. Each payment is its own decision, made
 * in library order — five separate "pay 2 life?" questions, exactly as the card is played.
 *
 * "Then put the rest into your hand" is folded into the pay branch: a card you paid for goes to
 * your hand as its own decision resolves rather than after the fifth. Nothing can observe the
 * difference — the cards never leave your library except into your own hand or graveyard, and
 * nothing triggers on a card entering a hand — while a trailing "move the rest" step would have
 * to re-find the cards still in the library, which the collection cannot do once some of it has
 * moved. The `fromZone` gate on both moves keeps a card that somehow left the library mid-loop
 * from being dragged along.
 */
val MoonlightBargain = card("Moonlight Bargain") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Look at the top five cards of your library. For each card, put that card into " +
        "your graveyard unless you pay 2 life. Then put the rest into your hand."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(5))
            run(
                Effects.ForEachInCollection(
                    looked,
                    Effects.MayPay(
                        cost = Effects.PayLife(2),
                        then = Effects.Move(EffectTarget.IterationEntity, Zone.HAND, fromZone = Zone.LIBRARY),
                        otherwise = Effects.Move(EffectTarget.IterationEntity, Zone.GRAVEYARD, fromZone = Zone.LIBRARY),
                        descriptionOverride = "Pay 2 life to put this card into your hand? " +
                            "If you don't, it goes to your graveyard.",
                    ),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "95"
        artist = "Nick Percival"
        flavorText = "At every fifth full moon, the Moon Market convenes to peddle Ravnica's most " +
            "forbidden wares."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/7957c37f-96da-4b7e-9581-afdf73a85edd.jpg?1783943667"
    }
}
