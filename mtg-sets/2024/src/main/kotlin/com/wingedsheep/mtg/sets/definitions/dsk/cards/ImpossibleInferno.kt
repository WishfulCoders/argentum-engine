package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Impossible Inferno
 * {4}{R}
 * Instant
 * Impossible Inferno deals 6 damage to target creature.
 * Delirium — If there are four or more card types among cards in your graveyard, exile the
 * top card of your library. You may play it until the end of your next turn.
 */
val ImpossibleInferno = card("Impossible Inferno") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Impossible Inferno deals 6 damage to target creature.\n" +
        "Delirium — If there are four or more card types among cards in your graveyard, exile the top card of your library. You may play it until the end of your next turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(6, t) then
            // Delirium: only exile/grant-play if there are four or more card types in your graveyard.
            Effects.If(
                condition = Conditions.Delirium(),
                then = Effects.Pipeline {
                    val impulseExiled = gather(CardSource.TopOfLibrary(1))
                    exile(impulseExiled)
                    run(Effects.GrantMayPlayFromExile(impulseExiled, MayPlayExpiry.UntilEndOfNextTurn))
                }
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Edgar Sánchez Hidalgo"
        flavorText = "No living being, survivor or monster, is exempt from Valgavoth's wrath."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a35248f9-9a4e-4758-a4c1-0e0c83e3fd75.jpg?1726286368"
    }
}
