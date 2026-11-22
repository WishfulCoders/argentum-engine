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
 * Vault of Champions — Commander Legends #360 (canonical printing)
 * Land
 *
 * This land enters tapped unless you have two or more opponents.
 * {T}: Add {W} or {B}.
 *
 * The battlebond-land shape (Luxury Suite): the opponent count is the current one.
 */
val VaultOfChampions = card("Vault of Champions") {
    manaCost = ""
    colorIdentity = "WB"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you have two or more opponents.\n{T}: Add {W} or {B}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.CompareAmounts(
                DynamicAmounts.playerCount(Player.EachOpponent), ComparisonOperator.GTE, 2
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "360"
        artist = "Cliff Childs"
        flavorText = "Where victors are commemorated and moments of glory immortalized."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e144ae1-500d-4485-b476-5783b14380d9.jpg?1783928738"
        ruling("2020-11-10", "Count the number of opponents you currently have, not how many you started with. If your four-player game is down to you and a single opponent, the land enters the battlefield tapped.")
        ruling("2020-11-10", "If an effect puts the land onto the battlefield tapped, having two or more opponents won't untap it.")
    }
}
