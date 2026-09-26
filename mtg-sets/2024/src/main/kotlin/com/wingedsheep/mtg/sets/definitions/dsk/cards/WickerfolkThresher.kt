package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Wickerfolk Thresher
 * {3}{G}
 * Artifact Creature — Scarecrow
 * 5/4
 *
 * Delirium — Whenever this creature attacks, if there are four or more card types among cards in
 * your graveyard, look at the top card of your library. If it's a land card, you may put it onto
 * the battlefield. If you don't put the card onto the battlefield, put it into your hand.
 *
 * Delirium is an ability word (no rules meaning of its own); the attack trigger carries an
 * intervening-"if" gate of [Conditions.Delirium] (four+ distinct card types in your graveyard).
 * The payoff is the look-top / play-land-else-hand pipeline (Fecund Greenshell shape), except the
 * land enters untapped ([ZonePlacement.Default]). Card types counted are artifact, battle,
 * creature, enchantment, instant, kindred, land, planeswalker, sorcery — supertypes and subtypes
 * don't count.
 */
val WickerfolkThresher = card("Wickerfolk Thresher") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Scarecrow"
    power = 5
    toughness = 4
    oracleText = "Delirium — Whenever this creature attacks, if there are four or more card types " +
        "among cards in your graveyard, look at the top card of your library. If it's a land card, " +
        "you may put it onto the battlefield. If you don't put the card onto the battlefield, put " +
        "it into your hand."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.Delirium()
        effect = Effects.Pipeline {
            // Look at the top card of your library.
            val looked = gather(CardSource.TopOfLibrary(1))
            // Split into land and non-land.
            val (landCards, nonLandCards) = filterSplit(looked, GameObjectFilter.Land)
            // If it's a land, you may put it onto the battlefield; else it stays for hand.
            val (toBattlefield, landToHand) = chooseUpToSplit(
                1,
                from = landCards,
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put into your hand"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD))
            // If you don't put the card onto the battlefield, put it into your hand
            // (both the declined land and any non-land top card).
            toHand(landToHand)
            toHand(nonLandCards)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "WolfSkullJack"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3a74892-20cd-47f7-b514-a4c7f14cca8b.jpg?1726286638"
    }
}
