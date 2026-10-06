package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Displacer Kitten
 * {3}{U}
 * Creature — Cat Beast
 * 2/2
 * Avoidance — Whenever you cast a noncreature spell, exile up to one target nonland permanent you
 * control, then return that card to the battlefield under its owner's control.
 *
 * "Avoidance" is an ability word with no rules meaning. The blink is the Flickering Hound shape:
 * exile, then return the same card as a new object (counters, Auras and damage gone; Equipment
 * falls off). A token exiled this way ceases to exist and does not return (2022-06-10 ruling). The
 * Kitten itself is a legal target — "nonland permanent you control" doesn't exclude it.
 */
val DisplacerKitten = card("Displacer Kitten") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Cat Beast"
    power = 2
    toughness = 2
    oracleText = "Avoidance — Whenever you cast a noncreature spell, exile up to one target nonland " +
        "permanent you control, then return that card to the battlefield under its owner's control."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        val permanent = target(TargetFilter(GameObjectFilter.NonlandPermanent.youControl()), optional = true)
        effect = Effects.Move(permanent, Zone.EXILE) then Effects.Move(permanent, Zone.BATTLEFIELD)
        description = "Avoidance — Whenever you cast a noncreature spell, exile up to one target nonland " +
            "permanent you control, then return that card to the battlefield under its owner's control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Campbell White"
        flavorText = "\"Isn't she the cutest thing you've almost seen?\"\n—Doen, purveyor of exotic pets"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7a401b8-29fb-46ef-a663-427f66724d5c.jpg?1783922795"
        ruling("2022-06-10", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling(
            "2022-06-10",
            "When the card returns to the battlefield, it will be a new object with no connection to the " +
                "card that was exiled. Auras attached to the exiled creature will be put into their " +
                "owners' graveyards. Any Equipment will become unattached and remain on the battlefield. " +
                "Any counters on the exiled permanent will cease to exist."
        )
    }
}
