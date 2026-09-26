package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Bandit's Talent
 * {1}{B}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * When this Class enters, each opponent discards two cards unless they discard a nonland card.
 * {B}: Level 2
 * At the beginning of each opponent's upkeep, if that player has one or fewer cards in hand,
 * they lose 2 life.
 * {3}{B}: Level 3
 * At the beginning of your draw step, draw an additional card for each opponent who has one or
 * fewer cards in hand.
 */
val BanditsTalent = card("Bandit's Talent") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Class"
    oracleText = "When this Class enters, each opponent discards two cards unless they discard a nonland card.\n" +
        "{B}: Level 2 — At the beginning of each opponent's upkeep, if that player has one or fewer cards " +
        "in hand, they lose 2 life.\n" +
        "{3}{B}: Level 3 — At the beginning of your draw step, draw an additional card for each opponent " +
        "who has one or fewer cards in hand."

    // Level 1: When this Class enters, each opponent discards two cards unless they discard a nonland card.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            effect = Patterns.Hand.discardCardsUnlessMatching(2, GameObjectFilter.Nonland)
        )
    }

    // Level 2: At the beginning of each opponent's upkeep, if that player has one or fewer
    // cards in hand, they lose 2 life.
    classLevel(2, "{B}") {
        triggeredAbility {
            trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
            interveningIf = Conditions.CompareAmounts(
                left = DynamicAmounts.count(Player.TriggeringPlayer, Zone.HAND),
                operator = ComparisonOperator.LTE,
                right = 1
            )
            effect = Effects.LoseLife(
                amount = 2,
                target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
            )
        }
    }

    // Level 3: At the beginning of your draw step, draw an additional card for each opponent
    // who has one or fewer cards in hand.
    classLevel(3, "{3}{B}") {
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.DRAW)
            effect = Effects.DrawCards(
                count = DynamicAmounts.countPlayersWith(
                    scope = Player.EachOpponent,
                    condition = Conditions.CompareAmounts(
                        left = DynamicAmounts.cardsInYourHand(),
                        operator = ComparisonOperator.LTE,
                        right = 1
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/485dc8d8-9e44-4a0f-9ff6-fa448e232290.jpg?1739659353"
    }
}
