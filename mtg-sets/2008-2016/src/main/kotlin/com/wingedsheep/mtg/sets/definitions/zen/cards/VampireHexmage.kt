package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vampire Hexmage
 * {B}{B}
 * Creature — Vampire Shaman
 * 2/1
 * First strike
 * Sacrifice this creature: Remove all counters from target permanent.
 *
 * Any permanent is a legal target, with or without counters; a planeswalker stripped of its
 * loyalty counters goes to the graveyard as a state-based action, and Dark Depths with no ice
 * counters triggers.
 */
val VampireHexmage = card("Vampire Hexmage") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Shaman"
    power = 2
    toughness = 1
    oracleText = "First strike\nSacrifice this creature: Remove all counters from target permanent."

    keywords(Keyword.FIRST_STRIKE)

    activatedAbility {
        cost = Costs.SacrificeSelf
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.RemoveAllCounters(permanent)
        description = "Sacrifice this creature: Remove all counters from target permanent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Eric Deschamps"
        flavorText = "When the blood hunt lost its thrill, she looked for less tangible means of domination."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93d2c4d1-6205-404a-b03d-995b90a3a33a.jpg?1783942148"

        ruling("2021-03-19", "Vampire Hexmage's ability can't target a permanent card in a zone other than the battlefield, such as a suspended card.")
        ruling("2021-03-19", "Any permanent can be targeted by the second ability, not just one with counters on it.")
    }
}
