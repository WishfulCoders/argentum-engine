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
 * Undergrowth Stadium — Commander Legends #359 (canonical printing)
 * Land
 *
 * This land enters tapped unless you have two or more opponents.
 * {T}: Add {B} or {G}.
 *
 * The battlebond-land shape (Luxury Suite): the opponent count is the current one.
 */
val UndergrowthStadium = card("Undergrowth Stadium") {
    manaCost = ""
    colorIdentity = "BG"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you have two or more opponents.\n{T}: Add {B} or {G}."

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
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "359"
        artist = "Yeong-Hao Han"
        flavorText = "To get to Valor's Reach, competitors must first make a name for themselves in the smaller arenas."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df64c9cf-17de-44f3-831d-ba09661f2308.jpg?1783928737"
        ruling("2020-11-10", "Count the number of opponents you currently have, not how many you started with. If your four-player game is down to you and a single opponent, the land enters the battlefield tapped.")
        ruling("2020-11-10", "If an effect puts the land onto the battlefield tapped, having two or more opponents won't untap it.")
    }
}
