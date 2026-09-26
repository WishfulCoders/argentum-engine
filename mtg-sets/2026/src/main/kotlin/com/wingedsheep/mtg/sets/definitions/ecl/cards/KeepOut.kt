package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Keep Out
 * {1}{W}
 * Instant
 *
 * Choose one —
 * • Keep Out deals 4 damage to target tapped creature.
 * • Destroy target enchantment.
 */
val KeepOut = card("Keep Out") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Keep Out deals 4 damage to target tapped creature.\n• Destroy target enchantment."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Keep Out deals 4 damage to target tapped creature") {
                val tappedCreature = target(TargetFilter.TappedCreature)
                effect = Effects.DealDamage(4, tappedCreature)
            },
            mode("Destroy target enchantment") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Ron Spencer"
        flavorText = "\"We don't want your kind round here.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4ab1601c-634c-4f21-8926-ba3cb92008c1.jpg?1767956928"
    }
}
