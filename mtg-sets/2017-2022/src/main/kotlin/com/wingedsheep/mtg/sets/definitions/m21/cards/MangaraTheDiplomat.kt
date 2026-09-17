package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mangara, the Diplomat
 * {3}{W}
 * Legendary Creature — Human Cleric
 * 2/4
 * Lifelink
 * Whenever an opponent attacks with creatures, if two or more of those creatures are attacking you
 * and/or planeswalkers you control, draw a card.
 * Whenever an opponent casts their second spell each turn, draw a card.
 *
 * Tomik, Wielder of Law's attack trigger and Howling Moon's second-spell trigger, each drawing one
 * card. As with Tomik, the intervening "if" counts the creatures attacking now: one that left the
 * battlefield in response no longer counts, where the ruling would still count it.
 */
val MangaraTheDiplomat = card("Mangara, the Diplomat") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Cleric"
    power = 2
    toughness = 4
    oracleText = "Lifelink\n" +
        "Whenever an opponent attacks with creatures, if two or more of those creatures are attacking you and/or planeswalkers you control, draw a card.\n" +
        "Whenever an opponent casts their second spell each turn, draw a card."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = TriggerSpec(
            event = EventPattern.CreaturesAttackYouEvent(
                minAttackers = 2,
                includePlaneswalkersYouControl = true,
            ),
            binding = TriggerBinding.ANY,
        )
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(
                Player.Each,
                GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers(),
            ).count(),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(2),
        )
        effect = Effects.DrawCards(1)
        description = "Whenever an opponent attacks with creatures, if two or more of those creatures are attacking you and/or planeswalkers you control, draw a card."
    }

    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.EachOpponent)
        effect = Effects.DrawCards(1)
        description = "Whenever an opponent casts their second spell each turn, draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "27"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b4e628f-5fc5-4c17-a07d-448d361d7e7c.jpg?1783930737"
        ruling("2020-06-23", "If your opponent attacks you with one creature and your planeswalker with another, you draw a card.")
        ruling("2020-06-23", "You draw just one card, no matter how many creatures are attacking you and your planeswalkers beyond the second.")
    }
}
