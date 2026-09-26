package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Finneas, Ace Archer
 * {G}{W}
 * Legendary Creature — Rabbit Archer
 * 2/2
 *
 * Reach, vigilance
 * Whenever Finneas attacks, put a +1/+1 counter on each other creature you control
 * that's a token or a Rabbit. Then if creatures you control have total power 10 or
 * greater, draw a card.
 */
val FinneasAceArcher = card("Finneas, Ace Archer") {
    manaCost = "{G}{W}"
    colorIdentity = "WG"
    typeLine = "Legendary Creature — Rabbit Archer"
    power = 2
    toughness = 2
    oracleText = "Reach, vigilance\nWhenever Finneas attacks, put a +1/+1 counter on each other creature you control that's a token or a Rabbit. Then if creatures you control have total power 10 or greater, draw a card."

    keywords(Keyword.REACH, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()

        // Put a +1/+1 counter on each other creature you control that's a token or a Rabbit
        // Then if creatures you control have total power 10 or greater, draw a card
        // "each other CREATURE … that's a token or a Rabbit" — noncreature tokens
        // (Food, Treasure, …) don't get counters
        val tokenOrRabbitFilter = GameObjectFilter.Creature.token() or GameObjectFilter.Creature.withSubtype("Rabbit")
        val otherTokenOrRabbitYouControl = GroupFilter(
            baseFilter = tokenOrRabbitFilter.youControl(),
            excludeSelf = true
        )

        effect = Effects.ForEachInGroup(
            filter = otherTokenOrRabbitYouControl,
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        ) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    left = DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Creature
                    ).sumPower(),
                    operator = ComparisonOperator.GTE,
                    right = 10
                ),
                then = Effects.DrawCards(1, EffectTarget.Controller)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dee197d-c313-4364-b52c-f83d5f579bc3.jpg?1721427047"
        ruling("2024-07-26", "Players can't take actions in between the time you put counters on creatures with Finneas's last ability and the point at which that ability checks the total power of creatures you control.")
    }
}
