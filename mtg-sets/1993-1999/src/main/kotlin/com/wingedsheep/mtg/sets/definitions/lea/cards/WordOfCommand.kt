package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Word of Command
 * {B}{B}
 * Instant
 * Look at target opponent's hand and choose a card from it. You control that player until Word of
 * Command finishes resolving. The player plays that card if able. While doing so, the player can
 * activate mana abilities only if they're from lands that player controls and only if mana they
 * produce is spent to activate other mana abilities of lands the player controls and/or to play
 * that card. If the chosen card is cast as a spell, you control the player while that spell is
 * resolving.
 *
 * Any card may be chosen; an unplayable choice simply does nothing ("if able"). A land is played
 * only on that player's turn with a land play left, using the ordinary land-play rules.
 */
val WordOfCommand = card("Word of Command") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Look at target opponent's hand and choose a card from it. You control that player " +
        "until Word of Command finishes resolving. The player plays that card if able. While doing so, " +
        "the player can activate mana abilities only if they're from lands that player controls and " +
        "only if mana they produce is spent to activate other mana abilities of lands the player " +
        "controls and/or to play that card. If the chosen card is cast as a spell, you control the " +
        "player while that spell is resolving."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.ControlPlayerDuringResolution(opponent))
            run(Effects.LookAtHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val chosen = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                prompt = "Choose a card for that player to play",
                showAllCards = true,
                alwaysPrompt = true,
            )
            val played = runStoringCollection { playedKey ->
                Effects.WithManaSpendingObligations(
                    Effects.WithManaAbilitySources(
                        Effects.ForcePlay(chosen, opponent, CollectionSlot(playedKey)),
                        GameObjectFilter.Land.youControl(),
                        opponent,
                    ),
                    opponent,
                )
            }
            run(Effects.ControlPlayerDuringResolution(opponent, played.asTarget))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96c21429-98d3-416b-be00-6aa9c4c5a006.jpg?1783948689"
    }
}
