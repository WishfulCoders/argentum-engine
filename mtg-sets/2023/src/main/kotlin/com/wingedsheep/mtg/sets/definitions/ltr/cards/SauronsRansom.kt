package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.LookAudience

/**
 * Sauron's Ransom
 * {1}{U}{B}
 * Instant
 *
 * Choose an opponent. They look at the top four cards of your library and separate
 * them into a face-down pile and a face-up pile. Put one pile into your hand and the
 * other into your graveyard. The Ring tempts you.
 *
 * A "divvy" variant (CR 700.3) with one concealed pile: the opponent looks at the top
 * four and partitions them, but only the face-up pile is shown to you when you choose
 * where each pile goes — the face-down pile stays hidden (you see only its size). The
 * Gather → Select → Reveal → ChoosePile → Move pipeline models this with masking alone,
 * no bespoke decision type:
 *  1. Gather the top four with [LookAudience.None] — the cards stay hidden from you (the
 *     caster); the opponent sees them only through their split decision below.
 *  2. The opponent separates them into a face-up pile (the cards they select) and a
 *     face-down pile (the rest).
 *  3. Publicly reveal the face-up pile (`revealed = true`) so you can see it when choosing;
 *     the face-down pile is never revealed, so it renders to you as opaque card backs.
 *  4. You choose which pile goes to your hand; the other goes to your graveyard.
 * Then "The Ring tempts you."
 *
 * "Choose an opponent" is the caster picking which opponent looks/partitions. It is modeled
 * with [Targets.Opponent] (the engine's mechanism for "choose an opponent", as on Faramir,
 * Prince of Ithilien); the split decision then uses [Chooser.TargetPlayer] so the chosen
 * opponent — not just "an opponent" — is the one who looks and separates the cards. In a
 * two-player game the lone opponent is the only legal choice.
 */
val SauronsRansom = card("Sauron's Ransom") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Choose an opponent. They look at the top four cards of your library and separate them into a face-down pile and a face-up pile. Put one pile into your hand and the other into your graveyard. The Ring tempts you."

    spell {
        val opponent = target(Targets.Opponent)
        // "Choose an opponent." — the caster picks which opponent looks and partitions.
        effect = Effects.Pipeline {
            // 1. The chosen opponent looks at the top four cards. LookAudience.None keeps
            //    them hidden from you; the opponent sees them only via their split below.
            val looked = gather(CardSource.TopOfLibrary(4), lookAudience = LookAudience.None)
            // 2. The chosen opponent separates them: the cards they select become the
            //    face-up pile, the rest the face-down pile.
            val (faceUp, faceDown) = chooseAnyNumberSplit(
                from = looked,
                chooser = Chooser.TargetPlayer,
                selectedLabel = "Face-up pile",
                remainderLabel = "Face-down pile",
                prompt = "Separate the top four cards into a face-up pile and a face-down pile. The cards you select are placed face up; the rest stay face down.",
                alwaysPrompt = true
            )
            // 3. Reveal the face-up pile to everyone (including the caster, who now sees it
            //    when choosing). The face-down pile is never revealed.
            val faceUpCards = gather(faceUp.asSource, revealed = true)
            // 4. You choose which pile goes to your hand; the other to your graveyard. You
            //    see the face-up pile's cards and only the size of the face-down pile.
            val (keepPile, otherPile) = choosePile(
                faceUpCards,
                faceDown,
                pileALabel = "Face-up pile",
                pileBLabel = "Face-down pile",
                chooser = Chooser.Controller,
                prompt = "Choose which pile goes to your hand; the other goes to your graveyard."
            )
            toHand(keepPile)
            toGraveyard(otherPile)
        } then Effects.TheRingTemptsYou()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Alex Brock"
        flavorText = "\"He was dear to you, I see. And now he shall endure the slow torment of years.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b98850c-ad69-42da-b91a-8dc5e226c444.jpg?1686970005"
    }
}
