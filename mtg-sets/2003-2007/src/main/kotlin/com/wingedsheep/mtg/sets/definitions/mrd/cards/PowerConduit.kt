package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Power Conduit — Mirrodin #229
 * {2} · Artifact
 *
 * {T}, Remove a counter from a permanent you control: Choose one —
 * • Put a charge counter on target artifact.
 * • Put a +1/+1 counter on target creature.
 *
 * The counter spent is of *any* kind off *any* permanent you control — `counterType = null` on
 * [Costs.RemoveCounters] means "any combination", which for a single counter is simply "the player
 * picks which one". The Conduit itself is a legal source (it's a permanent you control), so a
 * charge counter it put on itself can be recycled.
 *
 * The two modes are a printed "Choose one —" on an activated ability, so this is a plain
 * [ModalEffect.chooseOne] with per-mode targets — each mode demands its own target only when
 * chosen, and the ability is not a modal *spell* (nothing keys off that here).
 */
val PowerConduit = card("Power Conduit") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Remove a counter from a permanent you control: Choose one —\n" +
        "• Put a charge counter on target artifact.\n" +
        "• Put a +1/+1 counter on target creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveCounters(count = 1, counterType = null, filter = GameObjectFilter.Permanent)
        )
        effect = ModalEffect.chooseOne(
            mode("Put a charge counter on target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.AddCounters(CounterType.CHARGE, 1, artifact)
            },
            mode("Put a +1/+1 counter on target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
            }
        )
        description = "{T}, Remove a counter from a permanent you control: Choose one — " +
            "Put a charge counter on target artifact; or put a +1/+1 counter on target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Todd Lockwood"
        flavorText = "Never content, vedalken artificers continually tinker with their creations."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0f5c84f-1924-4a4a-84c1-00dcb756e9c9.jpg?1783944506"
    }
}
