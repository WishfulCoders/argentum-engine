package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jeweled Bird
 * {1}
 * Artifact
 * Remove this card from your deck before playing if you're not playing for ante.
 * {T}: Ante this artifact. If you do, put all other cards you own from the ante into your
 * graveyard, then draw a card.
 *
 * Only legal in a game played for ante (CR 407.3, enforced by deck validation). The bird is
 * traded for your whole contribution to the ante (its ruling): every *other* card you own there
 * goes to your graveyard. "If you do" is whether the bird is still on the battlefield to be anted —
 * one that left in response isn't, so nothing else happens. The other cards are gathered *before*
 * the bird moves: once anted it is a new object (CR 400.7), which a source-relative "other" filter
 * could no longer tell apart from the rest.
 */
val JeweledBird = card("Jeweled Bird") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Remove this card from your deck before playing if you're not playing for ante.\n" +
        "{T}: Ante this artifact. If you do, put all other cards you own from the ante into your " +
        "graveyard, then draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.If(
            Conditions.SourceInZone(Zone.BATTLEFIELD),
            Effects.Pipeline {
                val rest = gather(CardSource.FromZone(Zone.ANTE))
                run(Effects.Move(EffectTarget.Self, Zone.ANTE, fromZone = Zone.BATTLEFIELD))
                move(rest, CardDestination.ToZone(Zone.GRAVEYARD))
            } then Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfffb65d-851d-4dc9-9233-d53abf955dcd.jpg?1783948377"
    }
}
