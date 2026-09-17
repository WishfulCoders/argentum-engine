package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Path to Exile
 * {W}
 * Instant
 * Exile target creature. Its controller may search their library for a basic land card, put that
 * card onto the battlefield tapped, then shuffle.
 *
 * Price of Freedom's "its controller may search" pipeline after an exile: the exiled creature's
 * last controller decides, and declining skips the shuffle too (ruling).
 */
val PathToExile = card("Path to Exile") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature. Its controller may search their library for a basic land card, put that card onto the battlefield tapped, then shuffle."

    spell {
        target = TargetCreature()
        effect = Effects.Exile(EffectTarget.ContextTarget(0))
            .then(
                MayEffect(
                    effect = Effects.Composite(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.FromZone(
                                    zone = Zone.LIBRARY,
                                    player = Player.ControllerOf("target"),
                                    filter = GameObjectFilter.BasicLand,
                                ),
                                storeAs = "searchable",
                            ),
                            SelectFromCollectionEffect(
                                from = "searchable",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                                chooser = Chooser.ControllerOfTarget,
                                storeSelected = "found",
                            ),
                            MoveCollectionEffect(
                                from = "found",
                                destination = CardDestination.ToZone(
                                    zone = Zone.BATTLEFIELD,
                                    player = Player.ControllerOf("target"),
                                    placement = ZonePlacement.Tapped,
                                ),
                            ),
                            ShuffleLibraryEffect(target = EffectTarget.TargetController),
                        ),
                    ),
                    decisionMaker = EffectTarget.TargetController,
                ),
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
