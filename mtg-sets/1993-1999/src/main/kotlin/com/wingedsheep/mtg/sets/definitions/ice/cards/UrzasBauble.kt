package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming

/**
 * Urza's Bauble — Ice Age #343
 * {0} · Artifact · Uncommon
 *
 * {T}, Sacrifice this artifact: Look at a card at random in target player's hand. You draw a card
 * at the beginning of the next turn's upkeep.
 *
 * The look is Gather → Select(random) → Look: the target player's hand is gathered without being
 * shown to anyone, the engine picks one card at random (`SelectionMode.Random`), and `look` shows
 * only that card, and only to the activator (CR 701.20e) — nobody else learns which card it was.
 * An empty hand looks at nothing. The draw is Mishra's Bauble's step-based one-shot delayed trigger
 * at [Step.UPKEEP] with [DelayedTriggerTiming.NEXT_TURN]: "the next turn's upkeep" is whoever's turn
 * comes next, never the current turn even when activated during an upkeep, and the delayed trigger
 * belongs to the activator, so "you" draw.
 */
val UrzasBauble = card("Urza's Bauble") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice this artifact: Look at a card at random in target player's hand. " +
        "You draw a card at the beginning of the next turn's upkeep."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
            look(chooseRandom(1, from = hand))
        } then Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            effect = Effects.DrawCards(1),
            timing = DelayedTriggerTiming.NEXT_TURN
        )
        description = "{T}, Sacrifice this artifact: Look at a card at random in target player's hand. " +
            "You draw a card at the beginning of the next turn's upkeep."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "343"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58c9e9a7-e170-4361-b7d5-22fc0771c489.jpg?1783947454"
    }
}
