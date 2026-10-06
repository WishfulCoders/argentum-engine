package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blast Zone — War of the Spark #244
 * Land
 *
 * This land enters with a charge counter on it.
 * {T}: Add {C}.
 * {X}{X}, {T}: Put X charge counters on this land.
 * {3}, {T}, Sacrifice this land: Destroy each nonland permanent with mana value equal to the
 * number of charge counters on this land.
 *
 * The `{X}{X}` cost announces X and pays twice it, so the counters added are [DynamicAmounts.xValue]
 * (Panacea's shape). The sacrifice is a cost, so the sweep counts the charge counters the land
 * *had* when it was sacrificed — the last-known snapshot, as on The Filigree Sylex — not the zero a
 * graveyard card has.
 */
val BlastZone = card("Blast Zone") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "This land enters with a charge counter on it.\n" +
        "{T}: Add {C}.\n" +
        "{X}{X}, {T}: Put X charge counters on this land.\n" +
        "{3}, {T}, Sacrifice this land: Destroy each nonland permanent with mana value equal to the " +
        "number of charge counters on this land."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.CHARGE,
            count = 1,
            selfOnly = true,
        )
    )

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddColorlessMana(1)
        description = "{T}: Add {C}."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{X}"), Costs.Tap)
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.CHARGE,
            amount = DynamicAmounts.xValue(),
            target = EffectTarget.Self,
        )
        description = "{X}{X}, {T}: Put X charge counters on this land."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DestroyAll(
            GameObjectFilter.NonlandPermanent.manaValueEqualsDynamic(
                DynamicAmounts.lastKnownSourceCounters(CounterType.CHARGE)
            )
        )
        description = "{3}, {T}, Sacrifice this land: Destroy each nonland permanent with mana value " +
            "equal to the number of charge counters on this land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Chris Ostrowski"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea6bc7d5-e8f6-4103-920c-9f7ec5cd6c28.jpg?1783933369"
        ruling(
            "2025-07-25",
            "An activation cost of {X}{X} means that you pay twice X. If you want X to be 3, you pay {6} " +
                "to activate Blast Zone's ability."
        )
        ruling(
            "2025-07-25",
            "Every token has mana value 0 unless it is copying something or was created with a specific mana cost."
        )
        ruling(
            "2025-07-25",
            "If a permanent has {X} in its mana cost, X is 0 for the purpose of determining its mana value."
        )
    }
}
