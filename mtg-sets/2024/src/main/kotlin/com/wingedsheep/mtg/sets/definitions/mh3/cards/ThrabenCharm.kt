package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thraben Charm
 * {1}{W}
 * Instant
 * Choose one —
 * • Thraben Charm deals damage equal to twice the number of creatures you control to target creature.
 * • Destroy target enchantment.
 * • Exile any number of target players' graveyards.
 *
 * Standard `modal(chooseCount = 1)` charm shape. Mode 1's damage is
 * `DynamicAmount.Multiply(DynamicAmounts.creaturesYouControl(), 2)`. Mode 3 targets any number of
 * players (`TargetPlayer(unlimited = true)`) and iterates the chosen players with
 * [ForEachTargetEffect], gathering each one's graveyard (`CardSource.FromZone(Zone.GRAVEYARD,
 * Player.ContextPlayer(0))`) and moving it to exile — the Hollow Marauder per-target idiom.
 */
val ThrabenCharm = card("Thraben Charm") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Thraben Charm deals damage equal to twice the number of creatures you control to target creature.\n" +
        "• Destroy target enchantment.\n" +
        "• Exile any number of target players' graveyards."

    spell {
        modal(chooseCount = 1) {
            mode("Thraben Charm deals damage equal to twice the number of creatures you control to target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.DealDamage(
                    DynamicAmounts.creaturesYouControl() * 2,
                    creature,
                )
            }
            mode("Destroy target enchantment") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            }
            mode("Exile any number of target players' graveyards") {
                target(TargetPlayer(unlimited = true))
                effect = Effects.ForEachTarget(
                    Effects.Pipeline {
                    val tcGraveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.ContextPlayer(0)))
                    exile(tcGraveyard)
                },
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Carlos Palma Cruchaga"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd28a646-f38f-4cdf-948c-969cd979e5e6.jpg?1783911295"
    }
}
