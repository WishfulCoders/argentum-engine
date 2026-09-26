package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Gaea's Blessing
 * {1}{G}
 * Sorcery
 * Target player shuffles up to three target cards from their graveyard into their
 * library. Draw a card.
 * When Gaea's Blessing is put into your graveyard from your library, shuffle your
 * graveyard into your library.
 */
val GaeasBlessing = card("Gaea's Blessing") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target player shuffles up to three target cards from their graveyard into their library. Draw a card.\nWhen Gaea's Blessing is put into your graveyard from your library, shuffle your graveyard into your library."

    spell {
        // Target up to 3 cards from any graveyard, move them to owner's library,
        // shuffle, and draw a card.
        target = TargetObject(
            count = 3,
            optional = true,
            filter = TargetFilter.CardInGraveyard
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.LIBRARY)
        ) then Effects.ShuffleLibrary() then
            Effects.DrawCards(1)
    }

    // When this card is put into your graveyard from your library,
    // shuffle your graveyard into your library.
    triggeredAbility {
        trigger = Triggers.self.changesZone(from = Zone.LIBRARY, to = Zone.GRAVEYARD)
        triggerZone = Zone.GRAVEYARD
        effect = Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23cf81ed-b86c-42b8-b796-2032b0a3654a.jpg?1562732710"
    }
}
