package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lux Cannon
 * {4}
 * Artifact
 *
 * {T}: Put a charge counter on this artifact.
 * {T}, Remove three charge counters from this artifact: Destroy target permanent.
 *
 * The charge counters are spent straight off the source, so the removal is a self-scoped
 * [Costs.RemoveCounterFromSelf] atom rather than the distributed "from among permanents you
 * control" form.
 */
val LuxCannon = card("Lux Cannon") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Put a charge counter on this artifact.\n" +
        "{T}, Remove three charge counters from this artifact: Destroy target permanent."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        description = "{T}: Put a charge counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.CHARGE, 3)
        )
        val t = target(TargetFilter.Permanent)
        effect = Effects.Destroy(t)
        description = "{T}, Remove three charge counters from this artifact: Destroy target permanent."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "173"
        artist = "Martina Pilcerova"
        flavorText = "There are few problems that can't be solved by putting a hole in the world."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95e274ea-e8f6-48ea-a877-c84b77c96d0c.jpg?1783941704"
    }
}
