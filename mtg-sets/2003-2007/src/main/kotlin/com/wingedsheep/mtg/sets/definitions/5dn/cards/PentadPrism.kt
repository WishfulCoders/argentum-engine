package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Pentad Prism
 * {2}
 * Artifact
 * Sunburst (This artifact enters with a charge counter on it for each color of mana spent to cast it.)
 * Remove a charge counter from this artifact: Add one mana of any color.
 *
 * Sunburst on a noncreature permanent is "enters with a charge counter for each color of mana spent
 * to cast it" (CR 702.44a), i.e. [EntersWithDynamicCounters] fed by
 * [DynamicAmounts.colorsOfManaSpent] — the same count Converge reads. Colorless mana is not a color,
 * and a Prism put onto the battlefield without being cast spent no mana, so it enters with none.
 * The withdrawals are a plain mana ability whose only cost is a counter (Iceberg's shape); once the
 * counters are gone the Prism simply stays on the battlefield.
 *
 * Known gap: CR 702.44a gives +1/+1 counters instead of charge counters to a Sunburst object that
 * enters *as a creature*. The Prism only does that under an animate-artifacts effect (March of the
 * Machines), and the engine has no Sunburst keyword to switch the counter type, so that case still
 * gets charge counters.
 */
val PentadPrism = card("Pentad Prism") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Sunburst (This artifact enters with a charge counter on it for each color of mana spent to cast it.)\n" +
        "Remove a charge counter from this artifact: Add one mana of any color."

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.CHARGE,
            count = DynamicAmounts.colorsOfManaSpent()
        )
    )

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.CHARGE)
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "Remove a charge counter from this artifact: Add one mana of any color."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/672b9b16-daef-44e6-9a3a-cfd9f3c78bc7.jpg?1783944376"
        ruling(
            "2020-08-07",
            "Sunburst checks what mana was actually spent to cast the spell. If an effect allows you to spend mana \"as though it were mana\" of any color or type, that allows you to spend mana you couldn't otherwise spend, but it doesn't change what mana you spent to cast the spell."
        )
        ruling("2020-08-07", "Once Pentad Prism has run out of charge counters, it remains on the battlefield.")
    }
}
