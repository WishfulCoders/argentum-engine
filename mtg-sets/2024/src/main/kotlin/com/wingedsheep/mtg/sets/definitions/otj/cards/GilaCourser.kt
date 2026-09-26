package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Gila Courser
 * {2}{R}
 * Creature — Lizard Mount
 * 4/2
 * Whenever this creature attacks while saddled, exile the top card of your library.
 * Until the end of your next turn, you may play that card.
 * Saddle 1
 */
val GilaCourser = card("Gila Courser") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Mount"
    power = 4
    toughness = 2
    oracleText = "Whenever this creature attacks while saddled, exile the top card of your library. " +
        "Until the end of your next turn, you may play that card.\n" +
        "Saddle 1 (Tap any number of other creatures you control with total power 1 or more: " +
        "This Mount becomes saddled until end of turn. Saddle only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        triggerRestriction = Conditions.SourceIsSaddled
        effect = Effects.Pipeline {
            val exiledCard = gather(CardSource.TopOfLibrary(1))
            exile(exiledCard)
            run(Effects.GrantMayPlayFromExile(exiledCard, MayPlayExpiry.UntilEndOfNextTurn))
        }
    }

    keywordAbility(KeywordAbility.saddle(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f568803d-65c0-48d7-916f-671267a9e00e.jpg?1712355755"
    }
}
