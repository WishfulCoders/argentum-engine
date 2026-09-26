package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sonar Strike
 * {1}{W}
 * Instant
 *
 * Sonar Strike deals 4 damage to target attacking, blocking, or tapped creature.
 * You gain 3 life if you control a Bat.
 */
val SonarStrike = card("Sonar Strike") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Sonar Strike deals 4 damage to target attacking, blocking, or tapped creature. " +
        "You gain 3 life if you control a Bat."

    spell {
        val t = target(TargetFilter(GameObjectFilter.Creature.attackingOrBlockingOrTapped()))
        effect = Effects.DealDamage(4, t) then
            Effects.GainLife(
                DynamicAmounts.conditional(
                    condition = Conditions.ControlCreatureOfType(Subtype("Bat")),
                    ifTrue = 3,
                    ifFalse = 0
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Julie Dillon"
        flavorText = "Valley's midnight clerics channel the life force of other animals into blinding strikes and searing shrieks."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a50da179-751f-47a8-a547-8c4a291ed381.jpg?1721425953"
    }
}
