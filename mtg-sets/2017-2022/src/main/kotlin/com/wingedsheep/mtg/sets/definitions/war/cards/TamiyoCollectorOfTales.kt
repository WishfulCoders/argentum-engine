package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OpponentsCantMakeYouDiscard
import com.wingedsheep.sdk.scripting.OpponentsCantMakeYouSacrifice
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tamiyo, Collector of Tales
 * {2}{G}{U}
 * Legendary Planeswalker — Tamiyo
 * Starting Loyalty: 5
 *
 * Spells and abilities your opponents control can't cause you to discard cards or sacrifice
 * permanents.
 * +1: Choose a nonland card name, then reveal the top four cards of your library. Put all cards with
 * the chosen name from among them into your hand and the rest into your graveyard.
 * −3: Return target card from your graveyard to your hand.
 *
 * The static line is the pair of player-scoped "can't"s (CR 101.2) the engine reads separately —
 * [OpponentsCantMakeYouDiscard] and Sigarda's [OpponentsCantMakeYouSacrifice] — written as two
 * statics of this one permanent, so they come and go together. An opponent's discard or sacrifice
 * instruction aimed at Tamiyo's controller does nothing while the rest of that spell or ability
 * happens; an option to discard or sacrifice it offers ("unless you sacrifice", ward—discard, a chain
 * copy cost) can't be taken. Game-rule discards and deaths (cleanup hand size, lethal damage, the
 * legend rule) aren't caused by a spell or ability and are unaffected (the rulings).
 *
 * The +1 is Desperate Research's name-and-partition pipeline over the top four, with the misses going
 * to the graveyard. The −3 is Regrowth's targeted return; Tamiyo isn't in the graveyard while its
 * target is chosen (the last ruling).
 */
val TamiyoCollectorOfTales = card("Tamiyo, Collector of Tales") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Tamiyo"
    startingLoyalty = 5
    oracleText = "Spells and abilities your opponents control can't cause you to discard cards or " +
        "sacrifice permanents.\n" +
        "+1: Choose a nonland card name, then reveal the top four cards of your library. Put all " +
        "cards with the chosen name from among them into your hand and the rest into your graveyard.\n" +
        "−3: Return target card from your graveyard to your hand."

    staticAbility { ability = OpponentsCantMakeYouDiscard }
    staticAbility { ability = OpponentsCantMakeYouSacrifice }

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val chosenName = chooseCardName(prompt = "Choose a nonland card name", pool = CardNamePool.NONLAND)
            val revealed = gather(CardSource.TopOfLibrary(4, Player.You), revealed = true)
            val (matches, rest) = selectAllSplit(
                from = revealed,
                filter = GameObjectFilter.Any.namedFromVariable(chosenName)
            )
            toHand(matches)
            toGraveyard(rest)
        }
    }

    loyaltyAbility(-3) {
        val card = target(TargetFilter.CardInGraveyard.ownedByYou())
        effect = Effects.Move(card, Zone.HAND)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76776b24-a2e1-4590-88e7-8a421baf2fc4.jpg?1783933384"

        ruling("2019-05-03", "As a spell or ability an opponent controls resolves, if it would force you to sacrifice a permanent or discard a card, you just don't. That part of the effect does nothing. If that spell or ability gives you the option to sacrifice a permanent or to discard a card, you can't take that option.")
        ruling("2019-05-03", "If a spell or ability an opponent controls states that something happens unless you sacrifice a permanent (as Mogis, God of Slaughter does) or discard a card (as Painful Quandary does), you can't choose to sacrifice or discard. On the other hand, if a spell or ability an opponent controls instructs you to sacrifice a permanent unless you perform an action (as Killing Wave does) or discard a card unless you perform an action, you can choose whether or not to perform the action. If you don't perform the action, nothing happens, since the spell or ability can't cause you to sacrifice any permanents or discard cards.")
        ruling("2019-05-03", "Tamiyo's ability affects sacrifices, but not any other ways permanents can leave the battlefield. It won't stop a creature from dying due to lethal damage or having 0 toughness, and it won't stop a permanent from being put into its owner's graveyard due to the \"legend rule.\" None of these are sacrifices; they're the result of game rules.")
        ruling("2019-05-03", "If a spell or ability your opponent controls reduces your maximum hand size, Tamiyo's first ability won't stop you from discarding cards when the game rules cause you to discard during your cleanup step.")
        ruling("2019-05-03", "If Tamiyo has 3 loyalty, you can't activate her last ability to return her to your hand. She won't be in your graveyard while you're choosing targets for the ability.")
    }
}
