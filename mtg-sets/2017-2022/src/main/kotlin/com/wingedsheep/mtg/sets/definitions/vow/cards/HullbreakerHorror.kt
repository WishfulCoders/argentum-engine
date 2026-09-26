package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hullbreaker Horror
 * {5}{U}{U}
 * Creature — Kraken Horror
 * 7/8
 *
 * Flash
 * This spell can't be countered.
 * Whenever you cast a spell, choose up to one —
 * • Return target spell you don't control to its owner's hand.
 * • Return target nonland permanent to its owner's hand.
 */
val HullbreakerHorror = card("Hullbreaker Horror") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Kraken Horror"
    power = 7
    toughness = 8
    oracleText = "Flash\n" +
        "This spell can't be countered.\n" +
        "Whenever you cast a spell, choose up to one —\n" +
        "• Return target spell you don't control to its owner's hand.\n" +
        "• Return target nonland permanent to its owner's hand."

    keywords(Keyword.FLASH)
    cantBeCountered = true

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.Modal(
            modes = listOf(
                Mode.withTarget(
                    Effects.ReturnSpellToOwnersHand(),
                    TargetObject(filter = TargetFilter.SpellOnStack.opponentControls()),
                    "Return target spell you don't control to its owner's hand"
                ),
                mode("Return target nonland permanent to its owner's hand") {
                    val nonlandPermanent = target(TargetFilter.NonlandPermanent)
                    effect = Effects.ReturnToHand(nonlandPermanent)
                }
            ),
            chooseCount = 1,
            minChooseCount = 0
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b546bcf-2e86-42af-bf32-81c7fd36ef8c.jpg?1643587961"
    }
}
