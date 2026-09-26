package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Anafenza, Unyielding Lineage — Tarkir: Dragonstorm #2
 * {2}{W} · Legendary Creature — Spirit Soldier · 2/2
 *
 * Flash
 * First strike
 * Whenever another nontoken creature you control dies, Anafenza endures 2.
 * (Put two +1/+1 counters on it or create a 2/2 white Spirit creature token.)
 */
val AnafenzaUnyieldingLineage = card("Anafenza, Unyielding Lineage") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Spirit Soldier"
    power = 2
    toughness = 2
    oracleText = "Flash\n" +
        "First strike\n" +
        "Whenever another nontoken creature you control dies, Anafenza endures 2. " +
        "(Put two +1/+1 counters on it or create a 2/2 white Spirit creature token.)"

    keywords(Keyword.FLASH, Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsNontoken),
                    controllerPredicate = ControllerPredicate.ControlledByYou
                )).dies()
        effect = Effects.Endure(2)
        description = "Whenever another nontoken creature you control dies, Anafenza endures 2."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29957f49-9a6b-42f6-b2fb-b48f653ab725.jpg?1743203958"
    }
}
