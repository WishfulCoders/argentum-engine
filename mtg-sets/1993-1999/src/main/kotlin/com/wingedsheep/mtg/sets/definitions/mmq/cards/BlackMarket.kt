package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Black Market
 * {3}{B}{B}
 * Enchantment
 *
 * Whenever a creature dies, put a charge counter on this enchantment.
 * At the beginning of your first main phase, add {B} for each charge counter on this enchantment.
 *
 * Any creature dying counts — either player's. The mana trigger is the Altar of Shadows shape:
 * `beginningOf(Step.PRECOMBAT_MAIN)` (the first main phase) adding `countersOnSelf(CHARGE)` black
 * mana, which is not optional (2004-10-04 ruling).
 */
val BlackMarket = card("Black Market") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature dies, put a charge counter on this enchantment.\nAt the beginning of your first main phase, add {B} for each charge counter on this enchantment."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.AddMana(
            Color.BLACK,
            DynamicAmounts.countersOnSelf(CounterType.CHARGE)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Jeff Easley"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05976e5c-b46c-431e-9dbd-1dc5fad3536c.jpg?1783945957"
        ruling("2004-10-04", "Getting the mana is not optional.")
    }
}
