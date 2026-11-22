package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sky Crier — Streets of New Capenna #31
 * {1}{W} · Creature — Bird Citizen · 1/1
 *
 * Flying, lifelink
 * {3}{W}: You and target opponent each draw a card.
 */
val SkyCrier = card("Sky Crier") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Citizen"
    power = 1
    toughness = 1
    oracleText = "Flying, lifelink\n" +
        "{3}{W}: You and target opponent each draw a card."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        val opponent = target(Targets.Opponent)
        effect = Effects.DrawCards(1) then Effects.DrawCards(1, opponent)
        description = "{3}{W}: You and target opponent each draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Raoul Vitale"
        flavorText = "\"Breaking news! Lord Xander found dead! Suspect at large!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98e01fa9-9127-4250-94f1-39e71cc3c2bc.jpg?1783923151"
    }
}
