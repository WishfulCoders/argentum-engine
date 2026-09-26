package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Ghirapur Orrery
 * {4}
 * Artifact
 * Each player may play an additional land on each of their turns.
 * At the beginning of each player's upkeep, if that player has no cards in hand, that player draws three cards.
 *
 * The land drop is a symmetric [GrantAdditionalLandDrop] (`affected = Player.Each`); copies are
 * cumulative. The draw is an intervening-if (CR 603.4) on the upkeep player's hand, checked when
 * the upkeep begins and again on resolution — a second Orrery's trigger does nothing once the
 * first has refilled the hand.
 */
val GhirapurOrrery = card("Ghirapur Orrery") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Each player may play an additional land on each of their turns.\n" +
        "At the beginning of each player's upkeep, if that player has no cards in hand, that player draws three cards."

    staticAbility {
        ability = GrantAdditionalLandDrop(affected = Player.Each)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        // "That player" is the player whose upkeep it is — bound by the step trigger.
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.count(Player.TriggeringPlayer, Zone.HAND),
            ComparisonOperator.EQ,
            0,
        )
        effect = Effects.DrawCards(3, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "216"
        artist = "Kirsten Zirngibl"
        flavorText = "This detailed model allows the edificers to examine the city from every angle."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9bff744-873b-4fa1-8088-5f28bbcdc7b8.jpg?1783937155"
        ruling("2016-09-20", "Ghirapur Orrery's first ability allows a player to play an additional land during their main phase. Doing so follows the normal timing rules for playing lands.")
        ruling("2016-09-20", "If the player has any cards in hand as Ghirapur Orrery's second ability resolves, the ability does nothing. Notably, a second Ghirapur Orrery won't have a player draw another three cards unless the player empties their hand after resolving the first one's trigger.")
    }
}
