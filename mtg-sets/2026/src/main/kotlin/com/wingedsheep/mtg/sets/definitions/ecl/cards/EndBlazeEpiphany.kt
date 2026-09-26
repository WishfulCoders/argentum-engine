package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * End-Blaze Epiphany
 * {X}{R}
 * Instant
 * End-Blaze Epiphany deals X damage to target creature. When that creature dies this turn,
 * exile a number of cards from the top of your library equal to its power, then choose a
 * card exiled this way. Until the end of your next turn, you may play that card.
 */
val EndBlazeEpiphany = card("End-Blaze Epiphany") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "End-Blaze Epiphany deals X damage to target creature. When that creature dies this turn, " +
        "exile a number of cards from the top of your library equal to its power, then choose a card exiled this way. " +
        "Until the end of your next turn, you may play that card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), creature) then
            Effects.CreateDelayedTrigger(
                trigger = Triggers.self.dies(),
                watchedTarget = creature,
                expiry = DelayedTriggerExpiry.EndOfTurn,
                effect = Effects.Pipeline {
                    val exiled = gather(
                        CardSource.TopOfLibrary(
                            count = DynamicAmounts.triggeringPower()
                        )
                    )
                    exile(exiled)
                    val chosen = chooseExactly(1, from = exiled, prompt = "Choose a card you may play")
                    run(Effects.GrantMayPlayFromExile(
                        from = chosen,
                        expiry = MayPlayExpiry.UntilEndOfNextTurn
                    ))
                }
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Tyler Walpole"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f0a90ae-b3b3-4f52-8997-eac514b29e57.jpg?1767952140"
        ruling(
            "2025-11-17",
            "You pay all costs and follow all timing rules for cards played this way. For example, if the chosen " +
                "exiled card is a land card, you may play it only during your main phase while the stack is empty."
        )
    }
}
