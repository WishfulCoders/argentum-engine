package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Keeper of the Accord
 * {3}{W}
 * Creature — Human Soldier
 * 3/4
 *
 * At the beginning of each opponent's end step, if that player controls more creatures than you,
 * create a 1/1 white Soldier creature token.
 * At the beginning of each opponent's end step, if that player controls more lands than you, you
 * may search your library for a basic Plains card, put it onto the battlefield tapped, then shuffle.
 *
 * "That player" is the opponent whose end step it is — [Player.TriggeringPlayer] on a step trigger
 * (the Lavaborn Muse shape), not "an opponent". Both comparisons are intervening-ifs (CR 603.4):
 * checked as the end step begins and again on resolution (2020-11-10 ruling).
 */
val KeeperOfTheAccord = card("Keeper of the Accord") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 4
    oracleText = "At the beginning of each opponent's end step, if that player controls more creatures than you, create a 1/1 white Soldier creature token.\n" +
        "At the beginning of each opponent's end step, if that player controls more lands than you, you may search your library for a basic Plains card, put it onto the battlefield tapped, then shuffle."

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.END)
        interveningIf = thatPlayerControlsMore(GameObjectFilter.Creature)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/2/3/23b4c4d7-5b53-417a-a61b-df56eec4a274.jpg?1783928585",
        )
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.END)
        interveningIf = thatPlayerControlsMore(GameObjectFilter.Land)
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Denman Rooke"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37eec618-2c3d-423b-bbb7-72ef7deb38fc.jpg?1783928882"
        ruling("2020-11-10", "Both triggered abilities trigger at the same time. You may put them on the stack in either order.")
        ruling("2020-11-10", "If the opponent doesn't control more creatures than you as their end step begins, the first ability won't trigger. The same is true for lands and the second ability. Each of those abilities will check again when it tries to resolve. If the opponent no longer controls more creatures (or lands) than you, the ability won't resolve and will have no effect.")
    }
}

/** "that player controls more [filter] than you" — the end-step player against you. */
private fun thatPlayerControlsMore(filter: GameObjectFilter) = Conditions.CompareAmounts(
    DynamicAmounts.battlefield(Player.TriggeringPlayer, filter).count(),
    ComparisonOperator.GT,
    DynamicAmounts.battlefield(Player.You, filter).count(),
)
