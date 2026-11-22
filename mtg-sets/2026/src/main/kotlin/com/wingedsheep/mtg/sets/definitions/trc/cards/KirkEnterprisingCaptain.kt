package com.wingedsheep.mtg.sets.definitions.trc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kirk, Enterprising Captain
 * {3}{R}
 * Legendary Creature — Human Officer
 * 4/3
 * Menace
 * Whenever Kirk attacks while creatures you control have total power 8 or greater, exile the top
 * card of your library. You may play that card this turn.
 *
 * "Attacks while …" gates the trigger itself, so the total-power check is a `triggerRestriction`
 * made when Kirk attacks, not an intervening-if rechecked on resolution.
 */
val KirkEnterprisingCaptain = card("Kirk, Enterprising Captain") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Officer"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever Kirk attacks while creatures you control have total power 8 or greater, " +
        "exile the top card of your library. You may play that card this turn."
    power = 4
    toughness = 3

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            8
        )
        effect = Effects.Pipeline {
            val exiledCard = gather(CardSource.TopOfLibrary(1))
            exile(exiledCard)
            run(Effects.GrantMayPlayFromExile(exiledCard))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "198"
        artist = "Nathaniel Himawan"
        flavorText = "\"Risk is our business.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1d339e1-3433-4630-965e-1841e46b40a4.jpg?1784036716"
    }
}
