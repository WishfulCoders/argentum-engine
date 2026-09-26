package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fatal Fissure
 * {1}{B}
 * Instant
 *
 * Choose target creature. When that creature dies this turn, you earthbend 4.
 * (Target land you control becomes a 0/0 creature with haste that's still a land.
 * Put four +1/+1 counters on it. When it dies or is exiled, return it to the
 * battlefield tapped.)
 *
 * The spell only chooses the watched creature and installs a death-watch delayed
 * trigger (`Triggers.self.dies()` scoped to that creature via `watchedTarget`, expiring at
 * end of turn). The earthbend payoff lands on a land *you* control chosen when the
 * delayed trigger fires — modeled with `targetRequirement` (exposed to the effect as
 * `ContextTarget(0)`) feeding `Effects.Earthbend`. Earthbend is a keyword action
 * composed from existing primitives (animate land + haste + counters + return-tapped
 * self-triggers), not a keyword ability.
 */
val FatalFissure = card("Fatal Fissure") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose target creature. When that creature dies this turn, you earthbend 4. " +
        "(Target land you control becomes a 0/0 creature with haste that's still a land. " +
        "Put four +1/+1 counters on it. When it dies or is exiled, return it to the battlefield tapped.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.self.dies(),
            watchedTarget = creature,
            expiry = DelayedTriggerExpiry.EndOfTurn) {
            val land = target(TargetFilter.Land.youControl())
            effect = Effects.Earthbend(4, land)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Maojin Lee"
        flavorText = "Long Feng normally used the Dai Li to silence dissidents, but he wasn't above taking a more direct approach."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3343933d-4425-4ede-8d92-876bd0c6df60.jpg?1764120669"
    }
}
