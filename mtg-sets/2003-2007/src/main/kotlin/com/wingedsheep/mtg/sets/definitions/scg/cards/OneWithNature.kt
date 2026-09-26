package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * One with Nature
 * {G}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature deals combat damage to a player, you may search your library
 * for a basic land card, put that card onto the battlefield tapped, then shuffle.
 */
val OneWithNature = card("One with Nature") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhenever enchanted creature deals combat damage to a player, you may search your library for a basic land card, put that card onto the battlefield tapped, then shuffle."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = Filters.BasicLand,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2321b01c-7eef-48cc-a86b-4074dfa5b86b.jpg?1562526569"
    }
}
