package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * War Barge
 * {4}
 * Artifact
 * {3}: Target creature gains islandwalk until end of turn. When this artifact leaves the
 * battlefield this turn, destroy that creature. A creature destroyed this way can't be regenerated.
 *
 * The second sentence is a genuine delayed trigger, not a duration: it watches *this artifact*
 * leaving, and it is scheduled per activation, so ferrying two creatures schedules two watchers and
 * losing the Barge drowns both. `watchedTarget = Self` binds the watcher to the Barge at creation
 * time — which matters because by the time it fires the Barge is already gone.
 *
 * `expiry = EndOfTurn` is the "this turn" clause: a Barge that survives the turn owes nothing, and
 * next turn's passengers get their own fresh watcher.
 *
 * The passenger reference survives the round-trip as `ContextTarget(0)` — the ability's chosen
 * target, carried into the delayed trigger's effect.
 */
val WarBarge = card("War Barge") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{3}: Target creature gains islandwalk until end of turn. When this artifact " +
        "leaves the battlefield this turn, destroy that creature. A creature destroyed this way " +
        "can't be regenerated. (A creature with islandwalk can't be blocked as long as defending " +
        "player controls an Island.)"

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Mana("{3}")
        effect = Effects.GrantKeyword(Keyword.ISLANDWALK, creature) then
            Effects.CreateDelayedTrigger(
                trigger = Triggers.self.leaves(),
                watchedTarget = EffectTarget.Self,
                effect = Effects.Destroy(creature, noRegenerate = true),
                expiry = DelayedTriggerExpiry.EndOfTurn,
            )
        description = "{3}: Target creature gains islandwalk until end of turn. When this " +
            "artifact leaves the battlefield this turn, destroy that creature. A creature " +
            "destroyed this way can't be regenerated."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Tom Wänerstrand"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9023c078-4169-498b-8626-a4862e0631f8.jpg?1783947923"
    }
}
