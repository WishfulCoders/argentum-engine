package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Jaws of Defeat
 * {3}{B}
 * Enchantment
 * Whenever a creature you control enters, target opponent loses life equal to the difference
 * between that creature's power and its toughness.
 *
 * - "The difference" is the absolute difference (ruling 2025-04-04): a 3/5 and a 5/3 both give 2,
 *   composed as `max(P − T, T − P)`.
 * - Power and toughness are read on resolution off the triggering creature. If it left the
 *   battlefield in response, its last-known characteristics are used (CR 608.2h) — so a creature
 *   shrunk to death in response (Overkill's -0/-9999) still drains for the gap it died with.
 */
val JawsOfDefeat = card("Jaws of Defeat") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control enters, target opponent loses life equal to the " +
        "difference between that creature's power and its toughness."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        val opponent = target(Targets.Opponent)
        val power = DynamicAmounts.triggeringPower()
        val toughness = DynamicAmounts.triggeringToughness()
        effect = Effects.LoseLife(
            DynamicAmounts.max(power - toughness, toughness - power),
            opponent,
        )
        description = "Target opponent loses life equal to the difference between that creature's power " +
            "and its toughness."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Andreas Zafiratos"
        flavorText = "\"The Jeskai like talking about their Way? Let's see them talk their way out of " +
            "this dilemma.\"\n—Girdah, Abzan beastrider"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c495f3bf-86a4-4d2f-821f-c01515f084c9.jpg?1783907177"
        ruling(
            "2025-04-04",
            "To find the difference between a creature's power and its toughness, subtract the smaller " +
                "of those two numbers from the larger one. For example, the difference between the power " +
                "and toughness of a 3/5 creature is 2. The difference between the power and toughness of " +
                "a 5/3 creature is also 2."
        )
    }
}
