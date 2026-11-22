package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deadly Rollick — Commander 2020 #42 (canonical printing)
 * {3}{B} · Instant
 *
 * If you control a commander, you may cast this spell without paying its mana cost.
 * Exile target creature.
 *
 * Fierce Guardianship's free-cast gate: a `{0}` [SelfAlternativeCost] behind
 * [Conditions.YouControlACommander].
 */
val DeadlyRollick = card("Deadly Rollick") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "If you control a commander, you may cast this spell without paying its mana cost.\n" +
        "Exile target creature."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        condition = Conditions.YouControlACommander
    )

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Exile(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Izzy"
        flavorText = "Otrimi was disappointed at how quickly its new friend got tired and stopped playing."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c61fa2c0-63c0-4dc2-9f17-5a00530e3348.jpg?1783931217"
        ruling(
            "2020-04-17",
            "It doesn't matter whose commander you control. Any one will do. If you have two commanders, " +
                "you just need to control one of them."
        )
    }
}
