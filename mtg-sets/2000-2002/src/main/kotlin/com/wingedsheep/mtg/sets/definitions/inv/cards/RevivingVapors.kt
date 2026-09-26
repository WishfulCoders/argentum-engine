package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reviving Vapors
 * {2}{W}{U}
 * Instant
 *
 * Reveal the top three cards of your library and put one of them into your hand.
 * You gain life equal to that card's mana value. Put all other cards revealed this
 * way into your graveyard.
 *
 * Composed from the atomic reveal pipeline: Gather (top 3, revealed) → Select exactly 1
 * (caster's pick, remainder stored) → Move chosen to hand → gain life equal to its mana
 * value via [DynamicAmount.StoredCardManaValue] → move the remainder to the graveyard.
 */
val RevivingVapors = card("Reviving Vapors") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Reveal the top three cards of your library and put one of them into your hand. " +
        "You gain life equal to that card's mana value. Put all other cards revealed this way " +
        "into your graveyard."

    spell {
        effect = Effects.Pipeline {
            // 1. Reveal the top three cards of your library.
            val revealed = gather(CardSource.TopOfLibrary(3, Player.You), revealed = true)
            // 2. Put one of them into your hand (rest become the remainder).
            val (chosen, rest) = chooseExactlySplit(
                1,
                from = revealed,
                prompt = "Choose a card to put into your hand",
                alwaysPrompt = true
            )
            // 3. Move the chosen card to your hand.
            toHand(chosen)
            // 4. You gain life equal to that card's mana value.
            run(Effects.GainLife(DynamicAmounts.manaValueOf(chosen), EffectTarget.Controller))
            // 5. Put all other cards revealed this way into your graveyard.
            toGraveyard(rest)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "265"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47a23c32-e122-400b-b252-e636ea2e684b.jpg?1562909595"
    }
}
