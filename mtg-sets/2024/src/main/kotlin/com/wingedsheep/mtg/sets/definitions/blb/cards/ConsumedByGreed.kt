package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Consumed by Greed {1}{B}{B}
 * Instant
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 *
 * Target opponent sacrifices a creature with the greatest power among
 * creatures they control. If the gift was promised, return target creature
 * card from your graveyard to your hand.
 */
val ConsumedByGreed = card("Consumed by Greed") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\nTarget opponent sacrifices a creature with the greatest power among creatures they control. If the gift was promised, return target creature card from your graveyard to your hand."

    fun sacrificeGreatestPower(opponent: EffectTarget) = Effects.Sacrifice(
        filter = GameObjectFilter.Creature.hasGreatestPower(),
        count = 1,
        target = opponent
    )

    spell {
        effect = Patterns.Mechanic.giftSpell(
            // Mode 1: No gift — target opponent sacrifices creature with greatest power
            mode("Don't promise a gift — target opponent sacrifices a creature with the greatest power") {
                val opponent = target(Targets.Opponent)
                effect = sacrificeGreatestPower(opponent)
            },
            // Mode 2: Gift a card — opponent draws, sacrifice, return creature from graveyard
            mode("Promise a gift — opponent draws a card, target opponent sacrifices a creature with the greatest power, return target creature card from your graveyard to your hand") {
                val opponent = target(Targets.Opponent)
                val creatureCardInYourGraveyard = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.ChosenOpponent)) then
                    sacrificeGreatestPower(opponent) then
                    Effects.ReturnToHand(creatureCardInYourGraveyard) then
                    Effects.GiftGiven()
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e50acc41-3517-42db-b1d3-1bdfd7294d84.jpg?1721426362"
        ruling("2024-07-26", "If the target opponent has multiple creatures tied for the greatest power, that player chooses which one to sacrifice.")
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
    }
}
