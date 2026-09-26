package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Hellfire Mongrel
 * {2}{R}
 * Creature — Elemental Dog
 * 2/2
 * At the beginning of each opponent's upkeep, if that player has two or fewer cards in hand, this creature deals 2 damage to that player.
 *
 * Intervening-if (CR 603.4): the hand size is checked both when the trigger would fire and
 * again as it resolves. Same shape as Lavaborn Muse.
 */
val HellfireMongrel = card("Hellfire Mongrel") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Dog"
    power = 2
    toughness = 2
    oracleText = "At the beginning of each opponent's upkeep, if that player has two or fewer cards in hand, this creature deals 2 damage to that player."

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        // "That player" is the player whose upkeep it is — bound by the step trigger.
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.count(Player.TriggeringPlayer, Zone.HAND),
            ComparisonOperator.LTE,
            2,
        )
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Dan Murayama Scott"
        flavorText = "There is no fondness between the hound and its master. There is only a common appreciation of the hunt."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f975874-24a6-4a14-bc5c-898c3d8e90e5.jpg"
    }
}
