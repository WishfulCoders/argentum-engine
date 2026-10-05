package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Path to Exile
 * {W}
 * Instant
 * Exile target creature. Its controller may search their library for a basic land card, put that
 * card onto the battlefield tapped, then shuffle.
 *
 * The exiled creature's controller (last-known, CR 608.2h) — not Path's caster — makes the
 * optional search, so the [Effects.May] is delegated to [EffectTarget.TargetController] and the
 * whole search pipeline is scoped to [Player.ControllerOf]. Declining skips the shuffle too
 * (2026-01-27 ruling). Same shape as Erode.
 */
val PathToExile = card("Path to Exile") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature. Its controller may search their library for a basic land card, " +
        "put that card onto the battlefield tapped, then shuffle."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Exile(creature) then Effects.May(
            effect = Effects.Pipeline {
                val searchable = gather(
                    CardSource.FromZone(
                        zone = Zone.LIBRARY,
                        player = Player.ControllerOf("target"),
                        filter = GameObjectFilter.BasicLand,
                    ),
                    search = true
                )
                val found = chooseUpTo(1, from = searchable, chooser = Chooser.ControllerOfTarget)
                move(
                    found,
                    CardDestination.ToZone(
                        zone = Zone.BATTLEFIELD,
                        player = Player.ControllerOf("target"),
                        placement = ZonePlacement.Tapped,
                    )
                )
                run(Effects.ShuffleLibrary(target = EffectTarget.TargetController))
            },
            decisionMaker = EffectTarget.TargetController,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29b7a8b1-b98e-483a-87a4-73bd831c03d4.jpg?1783942491"
        ruling("2026-01-27", "The controller of the exiled creature isn't required to search their library for a basic land. If that player doesn't, the player won't shuffle their library.")
        ruling("2026-01-27", "If the target creature is an illegal target by the time Path to Exile tries to resolve, it won't resolve and none of its effects will happen. The creature's controller won't search for a basic land card.")
    }
}
