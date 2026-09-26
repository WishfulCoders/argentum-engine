package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dose of Dawnglow
 * {4}{B}
 * Instant
 *
 * Return target creature card from your graveyard to the battlefield.
 * Then if it isn't your main phase, blight 2.
 * (Put two -1/-1 counters on a creature you control.)
 */
val DoseOfDawnglow = card("Dose of Dawnglow") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Return target creature card from your graveyard to the battlefield. " +
        "Then if it isn't your main phase, blight 2. (Put two -1/-1 counters on a creature you control.)"

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefield(creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.IsYourMainPhase),
                then = Patterns.Mechanic.blight(2)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Quintin Gleim"
        flavorText = "A lifetime of mischief gave way to a moment of tenderness."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47414323-ca30-45b7-a0b2-6668312bee04.jpg?1765883451"
    }
}
