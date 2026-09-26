package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Jidoor, Aristocratic Capital // Overture
 * Land — Town // Sorcery — Adventure
 *
 * Jidoor, Aristocratic Capital:
 *   This land enters tapped.
 *   {T}: Add {U}.
 *
 * Overture — {4}{U}{U}, Sorcery — Adventure:
 *   Target opponent mills half their library, rounded down.
 *   (Then exile this card. You may play the land later from exile.)
 *
 * Town land // spell Adventure — see [IshgardTheHolySee] for the layout rationale. "Half their
 * library, rounded down" is `Divide(AggregateZone(ContextPlayer(0), LIBRARY), 2, roundUp = false)`
 * read off the chosen opponent (mirrors Rush of Dread's half-of-a-chosen-player counts).
 */
val JidoorAristocraticCapital = card("Jidoor, Aristocratic Capital") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Land — Town"
    oracleText = "This land enters tapped.\n{T}: Add {U}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    adventure("Overture") {
        manaCost = "{4}{U}{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Target opponent mills half their library, rounded down. " +
            "(Then exile this card. You may play the land later from exile.)"
        spell {
            val opponent = target(Targets.Opponent)
            effect = Patterns.Library.mill(
                count = DynamicAmounts.zone(opponent.asPlayer, Zone.LIBRARY).count() / 2,
                target = opponent
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "284"
        artist = "Erikas Perl"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98b2d5b5-f85b-4c42-a0f5-a76f6af304ba.jpg?1748962589"
    }
}
