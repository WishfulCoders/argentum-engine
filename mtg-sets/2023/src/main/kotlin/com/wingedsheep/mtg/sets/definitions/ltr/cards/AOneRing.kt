package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * A-The One Ring (Alchemy rebalance of [TheOneRing])
 * {4}
 * Legendary Artifact
 *
 * Indestructible
 * When The One Ring enters, if you cast it, you gain protection from everything until your next turn.
 * At the beginning of your upkeep, you lose 1 life for each burden counter on The One Ring.
 * {1}, {T}: Put a burden counter on The One Ring, then draw a card for each burden counter on The One Ring.
 *
 * Identical to the printed [TheOneRing] except the draw ability gains a {1} mana cost
 * (the Alchemy nerf). Reuses Gap 8 player-level protection — see [TheOneRing] for the full
 * engine notes.
 */
val AOneRing = card("A-The One Ring") {
    manaCost = "{4}"
    typeLine = "Legendary Artifact"
    oracleText = "Indestructible\n" +
        "When The One Ring enters, if you cast it, you gain protection from everything until your next turn.\n" +
        "At the beginning of your upkeep, you lose 1 life for each burden counter on The One Ring.\n" +
        "{1}, {T}: Put a burden counter on The One Ring, then draw a card for each burden counter on The One Ring."

    keywords(Keyword.INDESTRUCTIBLE)

    // When The One Ring enters, if you cast it, you gain protection from everything until your next turn.
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        effect = Effects.GrantPlayerProtection()
    }

    // At the beginning of your upkeep, you lose 1 life for each burden counter on The One Ring.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(
            DynamicAmounts.countersOnSelf(CounterType.BURDEN),
            EffectTarget.Controller
        )
    }

    // {1}, {T}: Put a burden counter on The One Ring, then draw a card for each burden counter on it.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.BURDEN, 1, EffectTarget.Self) then
            Effects.DrawCards(DynamicAmounts.countersOnSelf(CounterType.BURDEN))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "A-246"
        artist = "Veli Nyström"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23b91af8-eff9-402a-8ba8-ab470c5c1a44.jpg?1730837118"
    }
}
