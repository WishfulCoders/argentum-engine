package com.wingedsheep.mtg.sets.definitions.hml.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Memory Lapse — Homelands #32a (canonical printing)
 * {1}{U} · Instant
 *
 * Counter target spell. If that spell is countered this way, put it on top of its owner's library
 * instead of into that player's graveyard.
 *
 * A real counter into the library (`CounterDestination.Library`), like Hinder with only the top
 * as a destination: an uncounterable spell is untouched, "whenever a spell is countered" triggers
 * see it, and a flashback spell's own exile replacement still wins over the library.
 */
val MemoryLapse = card("Memory Lapse") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. If that spell is countered this way, put it on top of its " +
        "owner's library instead of into that player's graveyard."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpellToLibrary(LibraryChoicePosition.Top)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32a"
        artist = "Mark Tedin"
        flavorText = "\"Um . . . oh . . . what was I saying?\"\n—Reveka, Wizard Savant"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d2cc591-3a81-468a-91a4-3c3aac83a21a.jpg?1783947296"
        ruling(
            "2016-06-08",
            "Memory Lapse has a self-replacement effect that replaces the spell going to the graveyard " +
                "before any other effect can replace that event. If the spell was cast using flashback, " +
                "however, flashback will change the spell's destination from its owner's library to exile."
        )
    }
}
