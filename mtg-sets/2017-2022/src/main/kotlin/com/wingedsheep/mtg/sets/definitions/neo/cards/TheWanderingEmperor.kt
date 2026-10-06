package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoyaltyAbilitiesAtInstantSpeed
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Wandering Emperor — Kamigawa: Neon Dynasty #42 (canonical printing)
 * {2}{W}{W} · Legendary Planeswalker · Loyalty 3
 *
 * Flash
 * As long as The Wandering Emperor entered this turn, you may activate her loyalty abilities any
 * time you could cast an instant.
 * +1: Put a +1/+1 counter on up to one target creature. It gains first strike until end of turn.
 * −1: Create a 2/2 white Samurai creature token with vigilance.
 * −2: Exile target tapped creature. You gain 2 life.
 *
 * The second paragraph is [LoyaltyAbilitiesAtInstantSpeed] gated on
 * [Conditions.SourceEnteredThisTurn]: it lifts only the timing half of CR 606.3, so she still
 * activates one loyalty ability per turn (ruling 2022-02-18).
 */
val TheWanderingEmperor = card("The Wandering Emperor") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker"
    startingLoyalty = 3
    oracleText = "Flash\n" +
        "As long as The Wandering Emperor entered this turn, you may activate her loyalty abilities any time you could cast an instant.\n" +
        "+1: Put a +1/+1 counter on up to one target creature. It gains first strike until end of turn.\n" +
        "−1: Create a 2/2 white Samurai creature token with vigilance.\n" +
        "−2: Exile target tapped creature. You gain 2 life."

    keywords(Keyword.FLASH)

    staticAbility {
        condition = Conditions.SourceEnteredThisTurn
        ability = LoyaltyAbilitiesAtInstantSpeed
    }

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
    }

    loyaltyAbility(-1) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Samurai"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/f/6/f68e5337-6e44-4f8f-a102-2f97b433beea.jpg?1783923716",
        )
    }

    loyaltyAbility(-2) {
        val creature = target(TargetFilter.TappedCreature)
        effect = Effects.Exile(creature) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "42"
        artist = "Tommy Arnold"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fab2d8a9-ab4c-4225-a570-22636293c17d.jpg?1783923909"
        ruling("2022-02-18", "You may still only activate one of The Wandering Emperor's loyalty abilities on the turn she entered the battlefield.")
    }
}
