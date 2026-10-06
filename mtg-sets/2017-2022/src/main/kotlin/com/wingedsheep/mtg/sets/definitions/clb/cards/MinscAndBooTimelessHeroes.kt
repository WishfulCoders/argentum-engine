package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

private const val SACRIFICED_POWER = "minscSacrificedPower"
private const val SACRIFICED_HAMSTER = "minscSacrificedHamster"

/**
 * Minsc & Boo, Timeless Heroes — Commander Legends: Battle for Baldur's Gate #285
 * {2}{R}{G} · Legendary Planeswalker — Minsc · Loyalty 3
 *
 * When Minsc & Boo enters and at the beginning of your upkeep, you may create Boo, a legendary
 * 1/1 red Hamster creature token with trample and haste.
 * +1: Put three +1/+1 counters on up to one target creature with trample or haste.
 * −2: Sacrifice a creature. When you do, Minsc & Boo deals X damage to any target, where X is that
 *     creature's power. If the sacrificed creature was a Hamster, draw X cards.
 * Minsc & Boo, Timeless Heroes can be your commander.
 *
 * The −2 is a reflexive trigger (CR 603.12): the sacrifice happens as the loyalty ability resolves,
 * and only then is the damage target chosen. The sacrificed creature's power and Hamster-ness are
 * last-known information from the moment it was sacrificed (CR 608.2h) — so Boo pumped by the +1
 * deals its pumped power — read from the sacrifice snapshot inside the action and stored as numbers
 * in the pipeline that the reflexive ability inherits. If the reflexive ability's target is illegal
 * as it resolves, it does nothing at all — no damage and no cards (ruling 2022-06-10).
 * "Can be your commander" is read from the oracle text by the commander-eligibility check.
 */
val MinscAndBooTimelessHeroes = card("Minsc & Boo, Timeless Heroes") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Minsc"
    startingLoyalty = 3
    oracleText = "When Minsc & Boo enters and at the beginning of your upkeep, you may create Boo, a legendary 1/1 red Hamster creature token with trample and haste.\n" +
        "+1: Put three +1/+1 counters on up to one target creature with trample or haste.\n" +
        "−2: Sacrifice a creature. When you do, Minsc & Boo deals X damage to any target, where X is that creature's power. If the sacrificed creature was a Hamster, draw X cards.\n" +
        "Minsc & Boo, Timeless Heroes can be your commander."

    // "When Minsc & Boo enters and at the beginning of your upkeep, …" is one ability on two
    // events — two triggered-ability blocks sharing one effect (the Old Rutstein idiom).
    val createBoo = Effects.CreateToken(
        power = 1,
        toughness = 1,
        colors = setOf(Color.RED),
        creatureTypes = setOf("Hamster"),
        keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
        name = "Boo",
        legendary = true,
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d0475e9-68ae-4553-a5ef-650091e04967.jpg?1783922320",
    )
    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = createBoo
        description = "When Minsc & Boo enters, you may create Boo, a legendary 1/1 red Hamster " +
            "creature token with trample and haste."
    }
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        optional = true
        effect = createBoo
        description = "At the beginning of your upkeep, you may create Boo, a legendary 1/1 red " +
            "Hamster creature token with trample and haste."
    }

    loyaltyAbility(+1) {
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.withKeyword(Keyword.TRAMPLE) or
                    GameObjectFilter.Creature.withKeyword(Keyword.HASTE)
            ),
            optional = true
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, creature)
    }

    loyaltyAbility(-2) {
        val x = DynamicAmounts.storedNumber(SACRIFICED_POWER)
        effect = Effects.ReflexiveTrigger(
            action = Effects.SacrificeOwn(GameObjectFilter.Creature) then
                Effects.StoreNumber(SACRIFICED_POWER, DynamicAmounts.sacrificedPower()) then
                Effects.StoreNumber(
                    SACRIFICED_HAMSTER,
                    DynamicAmounts.conditional(Conditions.SacrificedHadSubtype("Hamster"), 1, 0)
                ),
            optional = false,
            descriptionOverride = "Sacrifice a creature. When you do, Minsc & Boo deals X damage to any target, " +
                "where X is that creature's power. If the sacrificed creature was a Hamster, draw X cards."
        ) {
            val anyTarget = target(Targets.Any)
            effect = Effects.DealDamage(x, anyTarget) then Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.storedNumber(SACRIFICED_HAMSTER), ComparisonOperator.GTE, 1
                ),
                then = Effects.DrawCards(x)
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "285"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/928036c9-11b8-493e-b9f2-8fbd3487cd19.jpg?1790212691"
        ruling("2022-06-10", "If there is no legal target for the reflexive trigger in Minsc & Boo's second loyalty ability, or if the target is illegal as the ability tries to resolve, you will not draw any cards even if the sacrificed creature was a Hamster.")
    }
}
