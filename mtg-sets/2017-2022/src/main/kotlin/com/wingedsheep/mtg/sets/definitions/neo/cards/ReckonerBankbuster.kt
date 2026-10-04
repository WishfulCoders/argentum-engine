package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.CrewSaddleCost
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Reckoner Bankbuster — Kamigawa: Neon Dynasty #255
 * {2} · Artifact — Vehicle · 4/4 · Rare
 *
 * This Vehicle enters with three charge counters on it.
 * {2}, {T}, Remove a charge counter from this Vehicle: Draw a card. Then if there are no charge
 * counters on this Vehicle, create a Treasure token and a 1/1 colorless Pilot creature token with
 * "This token crews Vehicles as though its power were 2 greater."
 * Crew 3
 *
 * The counter removal is part of the cost, so by resolution the count already reflects it: the
 * "then if" is an [Effects.If] reading [DynamicAmounts.countersOnSelf] after the draw (the
 * Darigaaz Reincarnated shape). The Pilot's crew bonus is the crew-only [CrewSaddleContribution]
 * Hotshot Mechanic prints, attached to the token as a static ability.
 */
val ReckonerBankbuster = card("Reckoner Bankbuster") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "This Vehicle enters with three charge counters on it.\n" +
        "{2}, {T}, Remove a charge counter from this Vehicle: Draw a card. Then if there are no charge " +
        "counters on this Vehicle, create a Treasure token and a 1/1 colorless Pilot creature token " +
        "with \"This token crews Vehicles as though its power were 2 greater.\"\n" +
        "Crew 3"

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.CHARGE,
            count = 3,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.CHARGE)
        )
        effect = Effects.DrawCards(1) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.countersOnSelf(CounterType.CHARGE),
                    ComparisonOperator.EQ,
                    0
                ),
                then = Effects.CreateTreasure(
                    imageUri = "https://cards.scryfall.io/normal/front/6/9/6911181d-573b-41eb-96a4-799c96e008fc.jpg?1783923711"
                ) then Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    creatureTypes = setOf("Pilot"),
                    imageUri = "https://cards.scryfall.io/normal/front/b/e/be84f259-2809-48c9-9c70-861437f08c23.jpg?1783923717",
                    staticAbilities = listOf(
                        CrewSaddleContribution(modifier = 2, costs = setOf(CrewSaddleCost.CREW))
                    )
                )
            )
        description = "{2}, {T}, Remove a charge counter: Draw a card. Then if there are no charge counters " +
            "on this Vehicle, create a Treasure token and a 1/1 Pilot creature token."
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "255"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/279acd17-6c17-427b-a69d-fc02442ff4a3.jpg?1783923822"
    }
}
