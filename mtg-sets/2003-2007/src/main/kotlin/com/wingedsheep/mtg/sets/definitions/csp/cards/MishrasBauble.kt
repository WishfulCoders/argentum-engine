package com.wingedsheep.mtg.sets.definitions.csp.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming

/**
 * Mishra's Bauble — Coldsnap #138
 * {0} · Artifact · Uncommon
 *
 * {T}, Sacrifice this artifact: Look at the top card of target player's library. Draw a card at
 * the beginning of the next turn's upkeep.
 *
 * The look is the Callous Deceiver shape aimed at another library (Eye Spy): a lone,
 * non-destructive gather of that player's top card, shown to the activator only. The draw is a
 * step-based one-shot delayed trigger at [Step.UPKEEP] with [DelayedTriggerTiming.NEXT_TURN] and
 * no `fireOnPlayer` — "the next turn's upkeep" is whoever's turn comes next, never the current
 * turn even when activated during an upkeep. The delayed trigger belongs to the activator, so the
 * Bauble's controller draws.
 */
val MishrasBauble = card("Mishra's Bauble") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice this artifact: Look at the top card of target player's library. " +
        "Draw a card at the beginning of the next turn's upkeep."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            gather(CardSource.TopOfLibrary(1, player.asPlayer))
        } then Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            effect = Effects.DrawCards(1),
            timing = DelayedTriggerTiming.NEXT_TURN
        )
        description = "{T}, Sacrifice this artifact: Look at the top card of target player's library. " +
            "Draw a card at the beginning of the next turn's upkeep."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Chippy"
        flavorText = "\"Arcum is a babbling fool! Phyrexian technology is our greatest blessing. " +
            "Take this delightful trinket for instance . . .\"\n—Heidar, Rimewind master"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a720448-017f-4f4a-9501-678245eaed17.jpg?1783943319"
    }
}
