package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Combat Professor
 * {3}{W}
 * Creature — Bird Cleric
 * 2/3
 * Flying
 * At the beginning of combat on your turn, target creature you control gets +1/+0 and gains vigilance until end of turn.
 */
val CombatProfessor = card("Combat Professor") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Cleric"
    power = 2
    toughness = 3
    oracleText = "Flying\nAt the beginning of combat on your turn, target creature you control gets +1/+0 and gains vigilance until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(1, 0, t) then Effects.GrantKeyword(Keyword.VIGILANCE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"Before Yovus was cut down at Pranticle Peak, she performed which maneuver? Anyone? From the reading?\""
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f669ac4-98ed-4e23-91a9-281f8277ab04.jpg?1783927394"
    }
}
