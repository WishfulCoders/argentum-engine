package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Conjurer's Closet
 * {5}
 * Artifact
 * At the beginning of your end step, you may exile target creature you control,
 * then return that card to the battlefield under your control.
 */
val ConjurersCloset = card("Conjurer's Closet") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your end step, you may exile target creature you control, then return that card to the battlefield under your control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.May(
            Effects.Move(creature, Zone.EXILE) then Effects.Move(creature, Zone.BATTLEFIELD)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7378e998-0382-42fc-8606-c6e7fc04b6a4.jpg?1782714431"
    }
}
