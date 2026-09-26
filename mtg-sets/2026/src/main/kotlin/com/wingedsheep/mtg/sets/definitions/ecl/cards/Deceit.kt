package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Targets

/**
 * Deceit
 * {4}{U/B}{U/B}
 * Creature — Elemental Incarnation
 * 5/5
 *
 * When this creature enters, if {U}{U} was spent to cast it, return up to one other
 * target nonland permanent to its owner's hand.
 * When this creature enters, if {B}{B} was spent to cast it, target opponent reveals
 * their hand. You choose a nonland card from it. That player discards that card.
 * Evoke {U/B}{U/B}
 */
val Deceit = card("Deceit") {
    manaCost = "{4}{U/B}{U/B}"
    colorIdentity = "UB"
    typeLine = "Creature — Elemental Incarnation"
    power = 5
    toughness = 5
    oracleText = "When this creature enters, if {U}{U} was spent to cast it, return up to one other target nonland permanent to its owner's hand.\n" +
        "When this creature enters, if {B}{B} was spent to cast it, target opponent reveals their hand. You choose a nonland card from it. That player discards that card.\n" +
        "Evoke {U/B}{U/B}"

    evoke = "{U/B}{U/B}"

    // Blue gate first (goes on stack first, resolves second)
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.ManaSpentToCastIncludes(requiredBlue = 2)
        val bounceTarget = target(TargetFilter.NonlandPermanent.other(), optional = true)
        effect = Effects.ReturnToHand(bounceTarget)
    }

    // Black gate second (goes on stack second, resolves first)
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.ManaSpentToCastIncludes(requiredBlack = 2)
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "212"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd82c9e4-9871-4e6d-b691-ee00b4b9a3c6.jpg?1759144842"
        ruling("2025-11-17", "Deceit's first and second abilities care about what mana was spent to pay its total cost, not just what mana was spent to pay the hybrid mana symbols in its cost.")
        ruling("2025-11-17", "Deceit's first and second abilities check to see if at least two mana of the appropriate colors were spent to pay Deceit's cost. It doesn't matter if more than two mana of that color was spent; the effect isn't multiplied.")
        ruling("2025-11-17", "If this spell is copied, the copy will not have had any colors of mana paid for it, no matter what colors were spent on the original spell.")
    }
}
