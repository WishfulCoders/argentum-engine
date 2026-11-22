package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Cyclonic Rift — Return to Ravnica #35 (canonical printing)
 * {1}{U} · Instant
 *
 * Return target nonland permanent you don't control to its owner's hand.
 * Overload {6}{U}
 *
 * "You don't control" is `Not(ControlledByYou)`, not "an opponent controls" — in Two-Headed Giant a
 * teammate's permanents are fair game too. Overloaded, the spell has no targets (CR 702.96b), so it
 * also returns hexproof and protected permanents.
 */
val CyclonicRift = card("Cyclonic Rift") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target nonland permanent you don't control to its owner's hand.\n" +
        "Overload {6}{U} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{6}{U}"))

    spell {
        val notYours = GameObjectFilter.NonlandPermanent.withControllerPredicate(
            ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
        )
        val permanent = target(TargetFilter(notYours))
        effect = Effects.ReturnToHand(permanent)

        overloadEffect = Patterns.Group.returnAllToHand(GroupFilter(notYours))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Chris Rahn"
        flavorText = "The Izzet specialize in unnatural disaster."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/205c4689-8b02-4d40-9274-3c1fcafa8b82.jpg?1783940370"
        ruling(
            "2024-01-12",
            "Because a spell with overload doesn't target when its overload cost is paid, it may affect " +
                "permanents with hexproof or with protection from the appropriate color."
        )
    }
}
