package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Deepfathom Echo — {2}{G}{U}
 * Creature — Merfolk Spirit
 * 4/4
 *
 * "At the beginning of combat on your turn, this creature explores. Then you may have it
 *  become a copy of another creature you control until end of turn."
 *
 * Implementation:
 *  - `trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)` fires at the start of combat on the controller's turn.
 *  - No target is declared at the `triggeredAbility` level. The "another creature you control"
 *    is selected mid-resolution — inside the `Effects.May` — via `Effects.SelectTarget`. This
 *    ensures the explore always runs unconditionally, and target selection happens only if the
 *    player accepts the copy step.
 *  - `Effects.Explore(EffectTarget.Self)` runs first (CR 701.44): reveals the top library card;
 *    a land goes to the hand, a nonland puts a +1/+1 counter on Deepfathom Echo and the
 *    controller may put that card into the graveyard.
 *  - `Effects.May(...)` wraps the copy step. The engine asks the controller yes/no; if yes:
 *    `Effects.SelectTarget(Targets.OtherCreatureYouControl, "copySource")` prompts for another
 *    creature the controller controls (Deepfathom Echo is excluded by `OtherCreatureYouControl`
 *    which carries `excludeSelf = true` via `TargetFilter.OtherCreatureYouControl`). When only
 *    one valid creature exists, the engine auto-selects it without a prompt.
 *  - `Effects.EachPermanentBecomesCopyOfTarget(target = PipelineTarget("copySource"),
 *    duration = EndOfTurn, affected = Self)` — single-permanent shape (`affected = Self`):
 *    Deepfathom Echo becomes a copy of the selected creature until end of turn, copiable values
 *    only (Rule 707). Counters, tapped state, attachments, and non-copy continuous effects are
 *    unaffected. End-of-turn cleanup restores Deepfathom Echo's original `CardComponent`
 *    snapshot via `CopyOfComponent`.
 */
val DeepfathomEcho = card("Deepfathom Echo") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Merfolk Spirit"
    power = 4
    toughness = 4
    oracleText = "At the beginning of combat on your turn, this creature explores. Then you may " +
        "have it become a copy of another creature you control until end of turn. (To have this " +
        "creature explore, reveal the top card of your library. Put that card into your hand if " +
        "it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or " +
        "put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        // Step 1: This creature explores (unconditional). Reveals top library card; land → hand,
        // nonland → +1/+1 counter on this creature + optional graveyard put (CR 701.44).
        effect = Effects.Explore(EffectTarget.Self) then
            // Step 2: The controller may have this creature become a copy of another creature
            // they control until end of turn. Target selection happens inside the Effects.May so
            // it is only asked when the player accepts, and does not bind at stack-placement time.
            Effects.May(
                Effects.Pipeline {
                    val copySource = selectTarget(TargetObject(filter = TargetFilter.OtherCreatureYouControl))
                    run(Effects.EachPermanentBecomesCopyOfTarget(
                        target = copySource.asTarget,
                        duration = Duration.EndOfTurn,
                        affected = EffectTarget.Self
                    ))
                }
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7c7fb87-8448-49f4-a9ed-db97f6a41d98.jpg?1782694427"
    }
}
