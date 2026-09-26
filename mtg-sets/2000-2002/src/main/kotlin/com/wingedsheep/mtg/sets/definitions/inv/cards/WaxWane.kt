package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wax // Wane (INV 296) — split-layout spell (CR 709).
 *
 * Wax {G} — Instant
 *   Target creature gets +2/+2 until end of turn.
 *
 * Wane {W} — Instant
 *   Destroy target enchantment.
 *
 * Cast either half from hand; only the chosen half goes on the stack (CR 709.4).
 */
val WaxWane = card("Wax // Wane") {
    layout = CardLayout.SPLIT
    colorIdentity = "GW"

    face("Wax") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +2/+2 until end of turn."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 2, creature)
        }
    }

    face("Wane") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Destroy target enchantment."

        spell {
            val enchantment = target(TargetFilter.Enchantment)
            effect = Effects.Destroy(enchantment)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "296"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19859061-f5ec-4b7f-86a1-196f98648e0a.jpg?1562900084"
    }
}
