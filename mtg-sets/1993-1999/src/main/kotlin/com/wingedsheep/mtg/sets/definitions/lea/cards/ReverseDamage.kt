package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Reverse Damage
 * {1}{W}{W}
 * Instant
 * The next time a source of your choice would deal damage to you this turn, prevent that damage.
 * You gain life equal to the damage prevented this way.
 *
 * The Deflecting Palm shape: the source is chosen on resolution (no target), the shield spends
 * itself on that source's next instance of damage to you (2004 ruling — a second hit isn't
 * reversed), and the life gain is the rest of the prevention effect, reading the amount the
 * shield actually prevented.
 */
val ReverseDamage = card("Reverse Damage") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "The next time a source of your choice would deal damage to you this turn, prevent " +
        "that damage. You gain life equal to the damage prevented this way."

    spell {
        effect = Effects.PreventDamage(
            sources = PreventionSourceFilter.Chosen(),
            onPrevented = Effects.GainLife(DynamicAmounts.preventedDamage())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Dameon Willich"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/943baea8-b173-4863-a3ab-dd217d483cd9.jpg?1783948711"
        ruling(
            "2004-10-04",
            "It only affects damage dealt by the source one time. If the source damages you a second " +
                "time this turn, the damage will not be reversed."
        )
    }
}
