package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Targets

/**
 * Psychic Whorl
 * {2}{B}
 * Sorcery
 * Target opponent discards two cards. Then if you control a Rat, surveil 2.
 */
val PsychicWhorl = card("Psychic Whorl") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards. Then if you control a Rat, surveil 2."

    spell {
        val t = target(Targets.Opponent)
        effect = Patterns.Hand.discardCards(2, t) then
            Effects.If(
                condition = Conditions.ControlCreatureOfType(Subtype("Rat")),
                then = Patterns.Library.surveil(2)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df900308-8432-4a0a-be21-17482026012b.jpg?1721426473"
    }
}
