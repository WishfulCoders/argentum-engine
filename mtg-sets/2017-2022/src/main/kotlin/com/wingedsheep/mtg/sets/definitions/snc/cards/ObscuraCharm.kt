package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Obscura Charm
 * {W}{U}{B}
 * Instant
 * Choose one —
 * • Return target multicolored permanent card with mana value 3 or less from your graveyard to the
 *   battlefield tapped.
 * • Counter target instant or sorcery spell.
 * • Destroy target creature or planeswalker with mana value 3 or less.
 */
val ObscuraCharm = card("Obscura Charm") {
    manaCost = "{W}{U}{B}"
    colorIdentity = "BUW"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Return target multicolored permanent card with mana value 3 or less from your graveyard to the battlefield tapped.\n" +
        "• Counter target instant or sorcery spell.\n" +
        "• Destroy target creature or planeswalker with mana value 3 or less."

    spell {
        modal(chooseCount = 1) {
            mode("Return target multicolored permanent card with mana value 3 or less from your graveyard to the battlefield tapped") {
                val t = target(
                    TargetFilter(
                        (GameObjectFilter.Permanent and GameObjectFilter.Multicolored)
                            .ownedByYou()
                            .manaValueAtMost(3),
                        zone = Zone.GRAVEYARD
                    )
                )
                effect = Effects.PutOntoBattlefieldFromGraveyard(t, tapped = true)
            }
            mode("Counter target instant or sorcery spell") {
                target(TargetFilter.InstantOrSorcerySpellOnStack)
                effect = Effects.CounterSpell()
            }
            mode("Destroy target creature or planeswalker with mana value 3 or less") {
                val t = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.manaValueAtMost(3)))
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9961562d-cad9-40e5-afae-3ebce77a2260.jpg?1783923075"
    }
}
