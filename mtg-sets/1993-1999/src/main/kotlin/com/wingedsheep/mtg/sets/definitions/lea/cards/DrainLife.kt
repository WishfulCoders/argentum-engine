package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Drain Life
 * {X}{1}{B}
 * Sorcery
 * Spend only black mana on X.
 * Drain Life deals X damage to any target. You gain life equal to the damage dealt, but not more
 * life than the player's life total before the damage was dealt, the planeswalker's loyalty before
 * the damage was dealt, or the creature's toughness.
 *
 * The cap is snapshotted *before* the damage (life total / loyalty / toughness of the target),
 * then the life gain is `min(cap, damage actually dealt)` — the dealt amount is read off the
 * `DamageDealtEvent`s, so prevention is accounted for. A creature's toughness is the cap even
 * when it is already damaged (2004-10-04 ruling). A battle target has no printed cap, so the
 * gain is the damage dealt (the cap slot falls back to X, which can never undercut it).
 */
val DrainLife = card("Drain Life") {
    manaCost = "{X}{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Spend only black mana on X.\n" +
        "Drain Life deals X damage to any target. You gain life equal to the damage dealt, but not " +
        "more life than the player's life total before the damage was dealt, the planeswalker's " +
        "loyalty before the damage was dealt, or the creature's toughness."

    spell {
        val anyTarget = target(Targets.Any)
        xManaRestriction = setOf(Color.BLACK)
        effect = Effects.Pipeline {
            val cap = storeNumber(
                DynamicAmounts.nonNegative(
                    DynamicAmounts.conditional(
                        Conditions.TargetIsPlayer(0),
                        DynamicAmounts.lifeTotal(anyTarget.asPlayer),
                        DynamicAmounts.conditional(
                            Conditions.TargetMatchesFilter(GameObjectFilter.Planeswalker, 0),
                            DynamicAmounts.countersOn(anyTarget, CounterType.LOYALTY),
                            DynamicAmounts.conditional(
                                Conditions.TargetMatchesFilter(GameObjectFilter.Creature, 0),
                                DynamicAmounts.toughnessOf(anyTarget),
                                DynamicAmounts.xValue()
                            )
                        )
                    )
                )
            )
            val dealt = runStoringNumber {
                Effects.DealDamage(DynamicAmounts.xValue(), anyTarget, damageDealtVariable = it)
            }
            run(Effects.GainLife(DynamicAmounts.min(cap.amount, dealt.amount)))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d077a49-73d4-4958-b42a-31b814e110e8.jpg?1783948696"
        ruling("2004-10-04", "You may gain up to the total toughness of the creature even if it was already damaged.")
    }
}
