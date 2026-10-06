package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Coalition Relic
 * {3}
 * Artifact
 * {T}: Add one mana of any color.
 * {T}: Put a charge counter on this artifact.
 * At the beginning of your first main phase, remove all charge counters from this artifact. Add one
 * mana of any color for each charge counter removed this way.
 *
 * The counter ability uses the stack (it adds no mana), so it is an ordinary activated ability. The
 * precombat-main trigger stores the charge count before stripping the counters (Lightning Coils'
 * "remove all of them … that many" shape) and adds that much mana in any combination of colors —
 * per the ruling, each mana may be a different color. The mana lands in the precombat main phase,
 * so it is there to spend for the rest of that phase.
 */
val CoalitionRelic = card("Coalition Relic") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color.\n" +
        "{T}: Put a charge counter on this artifact.\n" +
        "At the beginning of your first main phase, remove all charge counters from this artifact. " +
        "Add one mana of any color for each charge counter removed this way."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of any color."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        description = "{T}: Put a charge counter on this artifact."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.Pipeline {
            val removed = storeNumber(DynamicAmounts.countersOnSelf(CounterType.CHARGE))
            run(Effects.RemoveAllCountersOfType(CounterType.CHARGE, EffectTarget.Self))
            run(Effects.AddManaInAnyCombination(removed.amount))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a7c98b0-d64d-4d0a-b284-1187a8e7095e.jpg?1783943092"
        ruling(
            "2021-03-19",
            "If you remove multiple charge counters from Coalition Relic at once, you may add a different color of mana for each one."
        )
        ruling(
            "2021-03-19",
            "Only the first main phase each turn is considered a precombat main phase, even if additional main phases or combat phases are created."
        )
    }
}
