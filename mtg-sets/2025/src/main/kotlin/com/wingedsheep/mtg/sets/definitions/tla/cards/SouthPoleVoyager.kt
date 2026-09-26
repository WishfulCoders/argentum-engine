package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.dsl.Triggers

/**
 * South Pole Voyager
 * {1}{W}
 * Creature — Human Scout Ally
 * 2/2
 *
 * Whenever this creature or another Ally you control enters, you gain 1 life. If this is the
 * second time this ability has resolved this turn, draw a card.
 */
val SouthPoleVoyager = card("South Pole Voyager") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout Ally"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature or another Ally you control enters, you gain 1 life. If this is the second time this ability has resolved this turn, draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature
                    .withSubtype(Subtype("Ally"))
                    .youControl()).enters()
        effect = Effects.GainLife(1) then
            IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Effects.DrawCards(1)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Tition"
        flavorText = "\"The winds can be brutal, so be brave.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b5ad895-be8d-476b-91ca-22fad7a3cc58.jpg?1764120126"
    }
}
