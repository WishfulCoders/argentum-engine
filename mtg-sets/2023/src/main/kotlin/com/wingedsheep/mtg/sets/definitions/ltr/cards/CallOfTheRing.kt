package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Call of the Ring
 * {1}{B}
 * Enchantment
 *
 * At the beginning of your upkeep, the Ring tempts you.
 * Whenever you choose a creature as your Ring-bearer, you may pay 2 life. If you do, draw a card.
 *
 * The second ability uses the new `Triggers.you.isTemptedByTheRing(true)` (a `RingTemptedEvent`
 * pattern with `requireBearerChosen = true`), so it only fires when a temptation actually
 * designates a creature — not when you control none to choose.
 */
val CallOfTheRing = card("Call of the Ring") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, the Ring tempts you.\n" +
        "Whenever you choose a creature as your Ring-bearer, you may pay 2 life. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.TheRingTemptsYou()
    }

    triggeredAbility {
        trigger = Triggers.you.isTemptedByTheRing(true)
        effect = Effects.May(
            effect = Effects.LoseLife(2, EffectTarget.Controller) then Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Anato Finnstark"
        flavorText = "\"The Ring is mine!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a92a2c5a-e450-494a-b23b-7ac0a6c50535.jpg?1686968397"
    }
}
