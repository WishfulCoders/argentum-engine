package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jace, Wielder of Mysteries
 * {1}{U}{U}{U}
 * Legendary Planeswalker — Jace
 * Loyalty 4
 * If you would draw a card while your library has no cards in it, you win the game instead.
 * +1: Target player mills two cards. Draw a card.
 * −8: Draw seven cards. Then if your library has no cards in it, you win the game.
 *
 *  - The static is Laboratory Maniac's draw replacement: it applies only while Jace is on the
 *    battlefield, per draw, while the library is empty at that moment.
 *  - +1 is mill-then-draw in that order, so targeting yourself mills before the draw (2019-05-03
 *    ruling); an illegal player target fizzles the whole ability, draw included.
 *  - −8 at exactly eight loyalty leaves Jace in the graveyard before the ability resolves, so the
 *    replacement is gone: the seven draws run the library out (an empty-library draw is only
 *    flagged for the next state-based check, CR 704.5b), and the "then" check wins the game during
 *    resolution, before that check ever happens (2019-05-03 ruling).
 */
val JaceWielderOfMysteries = card("Jace, Wielder of Mysteries") {
    manaCost = "{1}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 4
    oracleText = "If you would draw a card while your library has no cards in it, you win the game instead.\n" +
        "+1: Target player mills two cards. Draw a card.\n" +
        "−8: Draw seven cards. Then if your library has no cards in it, you win the game."

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.WinGame(),
            restrictions = listOf(
                Exists(
                    player = Player.You,
                    zone = Zone.LIBRARY,
                    negate = true
                )
            )
        )
    )

    loyaltyAbility(+1) {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(2, player) then Effects.DrawCards(1)
    }

    loyaltyAbility(-8) {
        effect = Effects.DrawCards(7) then Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.zone(Player.You, Zone.LIBRARY).count(),
                ComparisonOperator.EQ,
                0
            ),
            then = Effects.WinGame(message = "Jace, Wielder of Mysteries: your library has no cards in it.")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb7d73-4482-4930-8497-cffd169b57e2.jpg?1783933463"
        ruling(
            "2019-05-03",
            "If for some reason you can't win the game (because your opponent controls Platinum Angel, " +
                "for example), you won't lose for having tried to draw a card from a library with no " +
                "cards in it. The draw was still replaced."
        )
        ruling(
            "2019-05-03",
            "If two or more players control Jace, Wielder of Mysteries and each player is instructed to " +
                "draw a number of cards, first the player whose turn it is draws that many cards. If this " +
                "causes that player to win the game instead, the game is immediately over. If the game " +
                "isn't over yet, repeat this process for each other player in turn order."
        )
        ruling(
            "2019-05-03",
            "If the target player is an illegal target when Jace's first loyalty ability tries to " +
                "resolve, it doesn't resolve. You won't draw a card."
        )
        ruling(
            "2019-05-03",
            "Follow the instructions in the order listed on Jace's first loyalty ability: if you target " +
                "yourself, you'll put the top two cards of your library into your graveyard and then draw " +
                "a card."
        )
        ruling(
            "2019-05-03",
            "If your library has fewer than seven cards in it while resolving Jace's last ability, and " +
                "Jace has already left the battlefield, you'll draw as many cards as you can and then win " +
                "the game before state-based actions would cause you to lose the game for trying to draw " +
                "from an empty library."
        )
    }
}
