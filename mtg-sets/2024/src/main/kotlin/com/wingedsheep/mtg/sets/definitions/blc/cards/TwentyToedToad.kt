package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Twenty-Toed Toad — Bloomburrow Commander #16
 * {3}{U} · Creature — Frog Wizard · 3/3
 *
 * Your maximum hand size is twenty.
 * Whenever you attack with two or more creatures, put a +1/+1 counter on this creature and draw a card.
 * Whenever this creature attacks, you win the game if there are twenty or more counters on it or
 * you have twenty or more cards in hand.
 *
 * The hand-size static competes with "no maximum hand size" effects in timestamp order (CR 613.11).
 * The win check counts counters of every kind on the Toad and is made only as the attack trigger
 * resolves; the trigger itself always goes on the stack.
 */
val TwentyToedToad = card("Twenty-Toed Toad") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Frog Wizard"
    power = 3
    toughness = 3
    oracleText = "Your maximum hand size is twenty.\n" +
        "Whenever you attack with two or more creatures, put a +1/+1 counter on this creature and draw a card.\n" +
        "Whenever this creature attacks, you win the game if there are twenty or more counters on it or you " +
        "have twenty or more cards in hand."

    staticAbility {
        ability = SetMaximumHandSize(Player.You, DynamicAmounts.fixed(20))
    }

    triggeredAbility {
        trigger = Triggers.you.attacks(minAttackers = 2)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.If(
            Conditions.Any(
                Conditions.CompareAmounts(DynamicAmounts.countersOnSelf(null), ComparisonOperator.GTE, 20),
                Conditions.CompareAmounts(DynamicAmounts.cardsInYourHand(), ComparisonOperator.GTE, 20),
            ),
            Effects.WinGame(EffectTarget.Controller),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/5479009c-9da5-4646-9894-fb9ec73dc9a9.jpg?1783910732"
        ruling(
            "2024-07-26",
            "If multiple effects modify your hand size, apply them in timestamp order. For example, if you put " +
                "Twenty-Toed Toad onto the battlefield and then put Spellbook (an artifact that says you have no " +
                "maximum hand size) onto the battlefield, you would have no maximum hand size. However, if those " +
                "permanents entered in the opposite order, your maximum hand size would be twenty."
        )
        ruling(
            "2024-07-26",
            "Twenty-Toed Toad's last ability will trigger whenever it attacks, no matter how many counters are on " +
                "it or cards you have in your hand at that time. The number of counters on it and the number of " +
                "cards in your hand are only checked when that ability resolves."
        )
    }
}
