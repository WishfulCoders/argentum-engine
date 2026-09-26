package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sorin, Imperious Bloodlord
 * {2}{B}
 * Legendary Planeswalker — Sorin
 * Starting Loyalty: 4
 *
 * +1: Target creature you control gains deathtouch and lifelink until end of turn. If it's a
 *     Vampire, put a +1/+1 counter on it.
 * +1: You may sacrifice a Vampire. When you do, Sorin deals 3 damage to any target and you gain
 *     3 life.
 * −3: You may put a Vampire creature card from your hand onto the battlefield.
 *
 * The first +1 grants deathtouch + lifelink to a targeted creature you control, then conditionally
 * adds a +1/+1 counter if that creature is a Vampire ([Conditions.EntityMatches]). The second +1 is
 * the optional-sacrifice reflexive pattern ([ReflexiveTriggerEffect] — Unscrupulous Contractor
 * shape): choose and sacrifice a Vampire you control, and "when you do" deal 3 to any target and
 * gain 3 life. The −3 puts an (optional, "up to one") Vampire creature card from hand onto the
 * battlefield via [Patterns.Hand.putFromHand] (ChooseUpTo = the "you may").
 */
val SorinImperiousBloodlord = card("Sorin, Imperious Bloodlord") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Sorin"
    startingLoyalty = 4
    oracleText = "+1: Target creature you control gains deathtouch and lifelink until end of turn. " +
        "If it's a Vampire, put a +1/+1 counter on it.\n" +
        "+1: You may sacrifice a Vampire. When you do, Sorin deals 3 damage to any target and you " +
        "gain 3 life.\n" +
        "−3: You may put a Vampire creature card from your hand onto the battlefield."

    loyaltyAbility(+1) {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, t, Duration.EndOfTurn) then
            Effects.GrantKeyword(Keyword.LIFELINK, t, Duration.EndOfTurn) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype("Vampire"), t),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t),
            )
    }

    loyaltyAbility(+1) {
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val vampireToSacrifice = selectTarget(
                    TargetObject(
                        filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Vampire").youControl())
                    )
                )
                run(Effects.SacrificeTarget(vampireToSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice a Vampire. When you do, Sorin deals 3 damage to " +
                "any target and you gain 3 life.",
        ) {
            val anyTarget = target(Targets.Any)
            effect = Effects.DealDamage(3, anyTarget) then Effects.GainLife(3)
        }
    }

    loyaltyAbility(-3) {
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature.withSubtype("Vampire")
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "115"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f764be3-dd3f-44b1-a4a6-807d1387590b.jpg?1782708313"
    }
}
