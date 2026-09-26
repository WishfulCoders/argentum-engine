package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Salt Road Skirmish — Tarkir: Dragonstorm #88
 * {3}{B} · Sorcery · Uncommon
 *
 * Destroy target creature. Create two 1/1 red Warrior creature tokens. They gain haste until
 * end of turn. Sacrifice them at the beginning of the next end step.
 *
 * The created tokens are modeled with built-in haste plus `sacrificeAtStep = Step.END`, which
 * schedules the sacrifice at the next end step — exactly the "haste now, gone at end of turn"
 * temporary-attacker shape used by Valduk's elementals.
 */
val SaltRoadSkirmish = card("Salt Road Skirmish") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. Create two 1/1 red Warrior creature tokens. They gain " +
        "haste until end of turn. Sacrifice them at the beginning of the next end step."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true) then
            Effects.CreateToken(
                count = 2,
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Warrior"),
                keywords = setOf(Keyword.HASTE),
                sacrificeAtStep = Step.END,
                imageUri = "https://cards.scryfall.io/normal/front/7/e/7edc0515-a130-45a7-aa09-0e23bba41587.jpg?1742506712"
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Arif Wijaya"
        flavorText = "Dariya's first mistake was thinking they could short the Mardu trader on a deal. " +
            "Their last was not considering the trader would bring backup."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f529a2e-5102-492e-84ab-68541d83b5a3.jpg?1743204314"
    }
}
