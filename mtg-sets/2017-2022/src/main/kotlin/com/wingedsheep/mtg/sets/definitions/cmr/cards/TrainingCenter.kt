package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Training Center — Commander Legends #358 (canonical printing)
 * Land
 *
 * This land enters tapped unless you have two or more opponents.
 * {T}: Add {U} or {R}.
 *
 * The battlebond-land shape (Luxury Suite): the opponent count is the current one.
 */
val TrainingCenter = card("Training Center") {
    manaCost = ""
    colorIdentity = "UR"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you have two or more opponents.\n{T}: Add {U} or {R}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.CompareAmounts(
                DynamicAmounts.playerCount(Player.EachOpponent), ComparisonOperator.GTE, 2
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "358"
        artist = "Daniel Ljunggren"
        flavorText = "Between matches, combatants practice to stay in peak mental and physical form."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b15c58c0-4d42-488e-bb9d-7a550d9ea5e6.jpg?1783928737"
        ruling("2020-11-10", "Count the number of opponents you currently have, not how many you started with. If your four-player game is down to you and a single opponent, the land enters the battlefield tapped.")
        ruling("2020-11-10", "If an effect puts the land onto the battlefield tapped, having two or more opponents won't untap it.")
    }
}
