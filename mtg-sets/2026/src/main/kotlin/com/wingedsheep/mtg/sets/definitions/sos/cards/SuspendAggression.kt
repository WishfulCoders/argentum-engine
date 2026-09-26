package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Suspend Aggression
 * {1}{R}{W}
 * Instant
 *
 * Exile target nonland permanent and the top card of your library. For each of those cards,
 * its owner may play it until the end of their next turn.
 *
 * The targeted permanent and your top library card are exiled into two collections (a permanent
 * may belong to any player, while the library card is always yours). Each card grants a
 * "may play from exile until the end of their next turn" permission to *its owner*
 * ([GrantMayPlayFromExileEffect.ownerControls]) — so the permanent's controller-opponent only
 * gets to replay the card they owned, and the turn-boundary expiry is measured per owner.
 */
val SuspendAggression = card("Suspend Aggression") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Exile target nonland permanent and the top card of your library. For each of " +
        "those cards, its owner may play it until the end of their next turn."

    spell {
        target(TargetFilter.NonlandPermanent)
        effect = Effects.Pipeline {
            // Exile the targeted nonland permanent; its owner may replay it.
            val suspendAggressionPermanent = gather(CardSource.ChosenTargets)
            exile(suspendAggressionPermanent)
            run(Effects.GrantMayPlayFromExile(
                from = suspendAggressionPermanent,
                expiry = MayPlayExpiry.UntilEndOfNextTurn,
                ownerControls = true
            ))
            // Exile the top card of your library; you may play it.
            val suspendAggressionTop = gather(CardSource.TopOfLibrary(1, Player.You))
            exile(suspendAggressionTop)
            run(Effects.GrantMayPlayFromExile(
                from = suspendAggressionTop,
                expiry = MayPlayExpiry.UntilEndOfNextTurn,
                ownerControls = true
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Andreas Zafiratos"
        flavorText = "\"Knowledge is your armor. Use it wisely, and it will keep you safe.\"\n—Augusta, dean of order"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/135c0696-d86d-4e48-988c-5c218de451fc.jpg?1775938648"
    }
}
