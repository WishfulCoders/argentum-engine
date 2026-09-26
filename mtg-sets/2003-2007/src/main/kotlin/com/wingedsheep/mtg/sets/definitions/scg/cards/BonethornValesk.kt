package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Bonethorn Valesk
 * {4}{R}
 * Creature — Beast
 * 4/2
 * Whenever a permanent is turned face up, Bonethorn Valesk deals 1 damage to any target.
 */
val BonethornValesk = card("Bonethorn Valesk") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 2
    oracleText = "Whenever a permanent is turned face up, Bonethorn Valesk deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.a().turnedFaceUp()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Alan Pollack"
        flavorText = "Barbarians weave its spurs into ceremonial charms to proclaim their hardiness in battle."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/297d7326-ad03-464d-97e2-443042d48f92.jpg?1562526649"
        ruling("2007-05-01", "Triggers when any permanent is turned face up, not just a creature.")
    }
}
