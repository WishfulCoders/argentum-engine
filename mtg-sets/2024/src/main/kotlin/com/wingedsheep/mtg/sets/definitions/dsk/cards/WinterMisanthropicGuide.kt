package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Winter, Misanthropic Guide
 * {1}{B}{R}{G}
 * Legendary Creature — Human Warlock
 * 3/4
 * Ward {2}
 * At the beginning of your upkeep, each player draws two cards.
 * Delirium — As long as there are four or more card types among cards in your graveyard, each
 * opponent's maximum hand size is equal to seven minus the number of those card types.
 */
val WinterMisanthropicGuide = card("Winter, Misanthropic Guide") {
    manaCost = "{1}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Human Warlock"
    power = 3
    toughness = 4
    oracleText = "Ward {2} (Whenever this creature becomes the target of a spell or ability an " +
        "opponent controls, counter it unless that player pays {2}.)\n" +
        "At the beginning of your upkeep, each player draws two cards.\n" +
        "Delirium — As long as there are four or more card types among cards in your graveyard, " +
        "each opponent's maximum hand size is equal to seven minus the number of those card types."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    // At the beginning of your upkeep, each player draws two cards.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.DrawCards(2, EffectTarget.PlayerRef(Player.Each))
    }

    // Delirium — As long as there are four or more card types among cards in your graveyard, each
    // opponent's maximum hand size is equal to seven minus the number of those card types.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = SetMaximumHandSize(
                player = Player.EachOpponent,
                amount = 7 - DynamicAmounts.zone(
                    Player.You,
                    Zone.GRAVEYARD,
                ).distinctTypes(),
            ),
            condition = Conditions.Delirium(),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Jodie Muir"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9b81421-44cb-440f-a6ac-3ddf620f1989.jpg?1726286766"
    }
}
