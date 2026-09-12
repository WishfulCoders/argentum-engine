package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Light Up the Stage
 * {2}{R}
 * Sorcery
 * Spectacle {R} (You may cast this spell for its spectacle cost rather than its mana cost if an
 * opponent lost life this turn.)
 * Exile the top two cards of your library. Until the end of your next turn, you may play those
 * cards.
 *
 * Spectacle is a [SelfAlternativeCost] gated on [Conditions.OpponentLostLifeThisTurn] (see Skewer
 * the Critics). The effect is Reckless Impulse's: [Patterns.Exile.impulse] with
 * [MayPlayExpiry.UntilEndOfNextTurn] — "play", so lands too, each on its normal timing, and a card
 * left unplayed stays in exile.
 */
val LightUpTheStage = card("Light Up the Stage") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Spectacle {R} (You may cast this spell for its spectacle cost rather than its mana cost if an opponent lost life this turn.)\n" +
        "Exile the top two cards of your library. Until the end of your next turn, you may play those cards."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{R}"),
        condition = Conditions.OpponentLostLifeThisTurn
    )

    spell {
        effect = Patterns.Exile.impulse(2, MayPlayExpiry.UntilEndOfNextTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Dmitry Burmak"
        flavorText = "\"Places, everyone!\"\n—Judith"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/9287b848-2aeb-4c70-ac4a-acafb871b7a4.jpg?1783933679"
        ruling("2024-01-12", "Spectacle cares only that an opponent lost life during the turn, not that the opponent's life total is currently lower than it was. For example, if an opponent loses 1 life and then gains 2 life in the same turn, you can cast a spell for its spectacle cost that turn.")
        ruling("2024-01-12", "Spectacle doesn't change when you can cast the spell. For example, you can't cast a sorcery with spectacle during an opponent's turn unless another effect allows you to do so, even if that player has lost life this turn.")
        ruling("2019-01-25", "If you don't play a card exiled this way, it remains in exile.")
        ruling("2019-01-25", "Light Up the Stage doesn't change when you can play the exiled cards. For example, if you exile a sorcery card, you can cast it only during your main phase when the stack is empty. If you exile a land card, you can play it only during your main phase and only if you have an available land play remaining.")
    }
}
