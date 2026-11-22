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
 * Rejuvenating Springs — Commander Legends #354 (canonical printing)
 * Land
 *
 * This land enters tapped unless you have two or more opponents.
 * {T}: Add {G} or {U}.
 *
 * The battlebond-land shape (Luxury Suite): the opponent count is the current one.
 */
val RejuvenatingSprings = card("Rejuvenating Springs") {
    manaCost = ""
    colorIdentity = "GU"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you have two or more opponents.\n{T}: Add {G} or {U}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.CompareAmounts(
                DynamicAmounts.playerCount(Player.EachOpponent), ComparisonOperator.GTE, 2
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "354"
        artist = "Alayna Danner"
        flavorText = "When they're done pummeling each other, the fighters relax companionably in the springs, warming their sore muscles."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51e69910-0d90-48a0-af29-3cddaeec5151.jpg?1783928741"
        ruling("2020-11-10", "Count the number of opponents you currently have, not how many you started with. If your four-player game is down to you and a single opponent, the land enters the battlefield tapped.")
        ruling("2020-11-10", "If an effect puts the land onto the battlefield tapped, having two or more opponents won't untap it.")
    }
}
