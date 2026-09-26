package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Iceberg
 * {X}{U}{U}
 * Enchantment
 *
 * This enchantment enters with X ice counters on it.
 * {3}: Put an ice counter on this enchantment.
 * Remove an ice counter from this enchantment: Add {C}.
 *
 * A mana battery, so the counters are the store and nothing else reads them. The cast-time X reaches the permanent through [EntersWithDynamicCounters] with a
 * [DynamicAmount.XValue] count — the replacement effect runs inside the permanent spell's own
 * resolution, where X is still live. Withdrawals are a plain mana ability whose cost is
 * `Costs.RemoveCounterFromSelf`, so they can be activated while paying for something else.
 */
val Iceberg = card("Iceberg") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "This enchantment enters with X ice counters on it.\n" +
        "{3}: Put an ice counter on this enchantment.\n" +
        "Remove an ice counter from this enchantment: Add {C}."

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.ICE,
            count = DynamicAmounts.xValue()
        )
    )

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.AddCounters(CounterType.ICE, 1, EffectTarget.Self)
        description = "{3}: Put an ice counter on this enchantment."
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.ICE)
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "Remove an ice counter from this enchantment: Add {C}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2f70e49-17fa-4033-bd45-63374f7f5ec5.jpg"
    }
}
