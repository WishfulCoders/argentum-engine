package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.dsl.Targets

/**
 * Binding Negotiation
 * {1}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You may choose a nonland card from it. If you do,
 * they discard it. Otherwise, you may put a face-up exiled card they own into their
 * graveyard.
 *
 * Modeled entirely from atomic pipeline primitives:
 *  - Reveal → gather the opponent's hand → `ChooseUpTo(1)` nonland (the "you may choose")
 *    → discard. The optional selection stores `toDiscard`; an empty selection means no
 *    card was chosen.
 *  - The "Otherwise" half is a resolution-time state test ([Effects.If], lowering to
 *    `Gate.WhenCondition`) gated on `toDiscard` being empty — i.e. nothing was discarded.
 *    It gathers the opponent's *face-up* exiled cards, offers an optional choice, and moves
 *    the chosen card to its owner's (the opponent's) graveyard.
 */
val BindingNegotiation = card("Binding Negotiation") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You may choose a nonland card from it. " +
        "If you do, they discard it. Otherwise, you may put a face-up exiled card they own into their graveyard."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            // Reveal the opponent's hand.
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            // "You may choose a nonland card from it." — optional (ChooseUpTo 1).
            val toDiscard = chooseUpTo(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "You may choose a nonland card for them to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            // "If you do, they discard it."
            discard(toDiscard, opponent.asPlayer)
            // "Otherwise, you may put a face-up exiled card they own into their graveyard."
            // Fires only if no card was discarded (toDiscard is empty).
            run(Effects.If(
                condition = Conditions.Not(whenMatches(toDiscard)),
                then = Effects.Pipeline {
                    val theirExile = gather(
                        CardSource.FromZone(
                            zone = Zone.EXILE,
                            player = opponent.asPlayer,
                            filter = GameObjectFilter.Any.faceUp()
                        )
                    )
                    val toBin = chooseUpTo(
                        1,
                        from = theirExile,
                        chooser = Chooser.Controller,
                        prompt = "You may put a face-up exiled card they own into their graveyard",
                        alwaysPrompt = true,
                        showAllCards = true
                    )
                    move(
                        toBin,
                        CardDestination.ToZone(Zone.GRAVEYARD, opponent.asPlayer),
                        moveType = MoveType.Default
                    )
                }
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Caroline Gariba"
        flavorText = "\"The tighter I bind them, the looser their tongues become.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c4c26b9-981f-47cf-b0f4-769e788d9537.jpg?1712355546"
    }
}
