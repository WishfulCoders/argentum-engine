package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Light of Hope
 * {W}
 * Instant
 * Choose one —
 * • You gain 4 life.
 * • Destroy target enchantment.
 * • Put a +1/+1 counter on target creature.
 */
val LightOfHope = card("Light of Hope") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n• You gain 4 life.\n• Destroy target enchantment.\n• Put a +1/+1 counter on target creature."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                effect = Effects.GainLife(4),
                description = "You gain 4 life."
            ),
            mode("Destroy target enchantment.") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            },
            mode("Put a +1/+1 counter on target creature.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(
                    CounterType.PLUS_ONE_PLUS_ONE,
                    1,
                    creature
                )
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Kimonas Theodossiou"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcb00599-e082-49b0-88f3-ef91b75595e4.jpg?1783931088"
    }
}
