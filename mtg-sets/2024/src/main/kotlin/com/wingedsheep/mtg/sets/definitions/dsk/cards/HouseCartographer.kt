package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.core.Step

/**
 * House Cartographer
 * {1}{G}
 * Creature — Human Scout Survivor
 * 2/2
 * Survival — At the beginning of your second main phase, if this creature is tapped, reveal
 * cards from the top of your library until you reveal a land card. Put that card into your hand
 * and the rest on the bottom of your library in a random order.
 *
 * "Survival" is an ability word (no rules meaning) — modeled as a postcombat-main-phase trigger
 * (`Triggers.you.beginningOf(Step.POSTCOMBAT_MAIN)`) with an intervening-if ([Conditions.SourceIsTapped], CR 603.4 —
 * checked both when it would trigger and on resolution). The reveal-until-land body reuses the
 * Clifftop Lookout pipeline (GatherUntilMatch → Reveal → Filter → Move), but lands the found card
 * in hand rather than onto the battlefield.
 */
val HouseCartographer = card("House Cartographer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout Survivor"
    power = 2
    toughness = 2
    oracleText = "Survival — At the beginning of your second main phase, if this creature is tapped, reveal cards from the top of your library until you reveal a land card. Put that card into your hand and the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.POSTCOMBAT_MAIN)
        interveningIf = Conditions.SourceIsTapped
        effect = Effects.Pipeline {
            val (revealedLand, allRevealed) = gatherUntilMatch(GameObjectFilter.Land)
            reveal(allRevealed)
            // allRevealed includes the matched land, so subtract it before bottoming.
            val nonLandRevealed = exclude(allRevealed, minus = revealedLand)
            toHand(revealedLand)
            toLibraryBottom(nonLandRevealed, order = CardOrder.Random)
        }
        description = "Survival — At the beginning of your second main phase, if this creature is " +
            "tapped, reveal cards from the top of your library until you reveal a land card. Put " +
            "that card into your hand and the rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a534918-a009-4f7d-87c9-5ef600b6e7c2.jpg?1726286553"
    }
}
