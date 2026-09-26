package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tragic Trajectory
 * {B}
 * Sorcery
 *
 * Target creature gets -2/-2 until end of turn.
 * Void — That creature gets -10/-10 until end of turn instead if a nonland permanent left the battlefield this turn
 * or a spell was warped this turn.
 */
val TragicTrajectory = card("Tragic Trajectory") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target creature gets -2/-2 until end of turn.\nVoid — That creature gets -10/-10 until end of turn instead if a nonland permanent left the battlefield this turn or a spell was warped this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            power = DynamicAmounts.conditional(
                condition = Conditions.Void,
                ifTrue = -10,
                ifFalse = -2
            ),
            toughness = DynamicAmounts.conditional(
                condition = Conditions.Void,
                ifTrue = -10,
                ifFalse = -2
            ),
            target = creature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Ovidio Cartagena"
        flavorText = "\"Rejoice, and know all will join you soon.\"\n—Calnan, Monoist assassin"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78c8bc60-1378-4028-8ab1-3286e459bffb.jpg?1752947046"
    }
}
