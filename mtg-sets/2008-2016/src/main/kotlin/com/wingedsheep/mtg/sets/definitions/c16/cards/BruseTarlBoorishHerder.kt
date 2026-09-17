package com.wingedsheep.mtg.sets.definitions.c16.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bruse Tarl, Boorish Herder
 * {2}{R}{W}
 * Legendary Creature — Human Ally
 * 3/3
 * Whenever Bruse Tarl enters or attacks, target creature you control gains double strike and
 * lifelink until end of turn.
 * Partner
 *
 * "Enters or attacks" is two triggers with one effect (both fire, ruling). Partner is omitted: the
 * engine does not model multi-commander decks.
 */
val BruseTarlBoorishHerder = card("Bruse Tarl, Boorish Herder") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Ally"
    power = 3
    toughness = 3
    oracleText = "Whenever Bruse Tarl enters or attacks, target creature you control gains double strike and lifelink until end of turn.\n" +
        "Partner (You can have two commanders if both have partner.)"

    for (trigger in listOf(Triggers.EntersBattlefield, Triggers.Attacks)) {
        triggeredAbility {
            this.trigger = trigger
            val t = target("target creature you control", Targets.CreatureYouControl)
            effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, t)
                .then(Effects.GrantKeyword(Keyword.LIFELINK, t))
            description = "Whenever Bruse Tarl enters or attacks, target creature you control gains double strike and lifelink until end of turn."
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "30"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/125b552b-45ea-4e0b-94a9-8131c97a04c0.jpg?1783937085"
        ruling("2020-11-10", "The triggered ability triggers both when Bruse Tarl enters the battlefield and whenever it attacks. You don't have to choose only one.")
    }
}
