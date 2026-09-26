package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step

/**
 * Sméagol, Helpful Guide
 * {1}{B}{G}
 * Legendary Creature — Halfling Horror
 * 4/2
 *
 * At the beginning of your end step, if a creature died under your control this turn, the
 * Ring tempts you.
 * Whenever the Ring tempts you, target opponent reveals cards from the top of their library
 * until they reveal a land card. Put that card onto the battlefield tapped under your control
 * and the rest into their graveyard.
 *
 * The reveal half composes GatherUntilMatch (until a land, from the target opponent's library)
 * + RevealCollection + two filtered MoveCollections: the land → your battlefield tapped, the
 * rest → the opponent's graveyard.
 */
val SmeagolHelpfulGuide = card("Sméagol, Helpful Guide") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Halfling Horror"
    power = 4
    toughness = 2
    oracleText = "At the beginning of your end step, if a creature died under your control this " +
        "turn, the Ring tempts you.\n" +
        "Whenever the Ring tempts you, target opponent reveals cards from the top of their " +
        "library until they reveal a land card. Put that card onto the battlefield tapped under " +
        "your control and the rest into their graveyard."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.ControlledCreatureDiedThisTurn
        effect = Effects.TheRingTemptsYou()
    }

    triggeredAbility {
        trigger = Triggers.you.isTemptedByTheRing()
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val (_, allRevealed) = gatherUntilMatch(GameObjectFilter.Land, player = Player.TargetOpponent)
            reveal(allRevealed)
            // The land enters under your control, tapped.
            move(
                allRevealed,
                CardDestination.ToZone(
                    Zone.BATTLEFIELD,
                    player = Player.You,
                    placement = ZonePlacement.Tapped
                ),
                filter = GameObjectFilter.Land
            )
            // The rest go into the opponent's graveyard (they own them).
            move(
                allRevealed,
                CardDestination.ToZone(
                    Zone.GRAVEYARD,
                    player = Player.TargetOpponent
                ),
                filter = GameObjectFilter.Nonland
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13253f8d-1897-41e8-a904-9e57ac7eff0a.jpg?1686970071"
    }
}
