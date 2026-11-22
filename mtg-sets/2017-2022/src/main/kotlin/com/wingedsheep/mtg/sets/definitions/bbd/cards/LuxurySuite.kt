package com.wingedsheep.mtg.sets.definitions.bbd.cards

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
 * Luxury Suite — Battlebond #82 (canonical printing)
 * Land
 *
 * This land enters tapped unless you have two or more opponents.
 * {T}: Add {B} or {R}.
 *
 * The Battlebond "battlebond land" cycle. "Two or more opponents" counts the opponents you have
 * *now* (`PlayerCount(EachOpponent)` reads `GameState.getOpponents`, which drops players who have
 * lost), so a four-player game down to a single opponent sees it enter tapped.
 */
val LuxurySuite = card("Luxury Suite") {
    manaCost = ""
    colorIdentity = "BR"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you have two or more opponents.\n{T}: Add {B} or {R}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.CompareAmounts(
                DynamicAmounts.playerCount(Player.EachOpponent), ComparisonOperator.GTE, 2
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
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
        collectorNumber = "82"
        artist = "Jonas De Ro"
        flavorText = "The view is rivaled only by the decadence of the decor."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81298b0b-9d47-4777-998e-0c17821ef536.jpg?1783934848"
        ruling("2018-06-08", "If you began the game with two or more opponents but now only have one opponent left, these lands enter the battlefield tapped.")
    }
}
