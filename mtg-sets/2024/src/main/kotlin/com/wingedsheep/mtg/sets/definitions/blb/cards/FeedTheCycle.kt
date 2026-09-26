package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets

/**
 * Feed the Cycle
 * {1}{B}
 * Instant
 *
 * As an additional cost to cast this spell, forage or pay {B}.
 * (To forage, exile three cards from your graveyard or sacrifice a Food.)
 * Destroy target creature or planeswalker.
 *
 * Modeled with [ModalEffect] for the binary cost fork (forage or pay {B}), but
 * the spell is NOT modal in MTG terms — there is no "Choose one — • X • Y"
 * wording. `countsAsModalSpell = false` keeps Riku of Many Paths and other
 * "Whenever you cast a modal spell" triggers from misreading it.
 * Mode 1 = pay extra {B} (total {1}{B}{B}) + destroy
 * Mode 2 = forage (total {1}{B} + forage) + destroy
 */
val FeedTheCycle = card("Feed the Cycle") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, forage or pay {B}. (To forage, exile three cards from your graveyard or sacrifice a Food.)\nDestroy target creature or planeswalker."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: Pay {B} additional — total mana cost becomes {1}{B}{B}
            mode("Pay {B} — destroy target creature or planeswalker") {
                val creatureOrPlaneswalker = target(Targets.CreatureOrPlaneswalker)
                additionalManaCost = "{B}"
                additionalCosts = emptyList()
                effect = Effects.Destroy(creatureOrPlaneswalker)
            },
            // Mode 2: Forage — total mana cost stays {1}{B} plus forage
            mode("Forage — destroy target creature or planeswalker") {
                val creatureOrPlaneswalker = target(Targets.CreatureOrPlaneswalker)
                additionalCosts = listOf(Costs.additional.Forage)
                effect = Effects.Destroy(creatureOrPlaneswalker)
            },
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Donato Giancola"
        flavorText = "Sleeping beneath the sumac\n—Squirrelfolk expression meaning \"death\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e017ff8-2936-4a1b-bece-00004cfbad06.jpg?1721426413"
    }
}
