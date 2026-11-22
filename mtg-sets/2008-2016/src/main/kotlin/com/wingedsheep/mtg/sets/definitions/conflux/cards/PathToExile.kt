package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Path to Exile — Conflux #15 (canonical printing)
 * {W} · Instant
 *
 * Exile target creature. Its controller may search their library for a basic land card, put that
 * card onto the battlefield tapped, then shuffle.
 *
 * "Its controller" is [Player.ControllerOf] the exiled creature, read from last-known information
 * once it has left the battlefield (CR 608.2h), so a stolen creature's fetch goes to whoever
 * controlled it, not its owner. The search runs under [Effects.ForEachPlayer], which rebinds
 * `Player.You` to that player; declining the "may" skips the shuffle too (ruling 2026-01-27).
 */
val PathToExile = card("Path to Exile") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature. Its controller may search their library for a basic land card, " +
        "put that card onto the battlefield tapped, then shuffle."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Exile(creature) then
            Effects.ForEachPlayer(
                Player.ControllerOf("target creature"),
                listOf(
                    Effects.May(
                        Patterns.Library.searchLibrary(
                            filter = GameObjectFilter.BasicLand,
                            count = 1,
                            destination = SearchDestination.BATTLEFIELD,
                            entersTapped = true
                        )
                    )
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29b7a8b1-b98e-483a-87a4-73bd831c03d4.jpg?1783942491"
        ruling(
            "2026-01-27",
            "The controller of the exiled creature isn't required to search their library for a basic land. " +
                "If that player doesn't, the player won't shuffle their library."
        )
    }
}
