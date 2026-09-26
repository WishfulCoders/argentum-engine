package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eye for an Eye
 * {W}{W}
 * Instant
 * The next time a source of your choice would deal damage to you this turn, instead that source
 * deals that much damage to you and Eye for an Eye deals that much damage to that source's
 * controller.
 *
 * Composition: this reuses the chosen-source damage-reaction machinery (Deflecting Palm), but with
 * `stillDealt = true` on the same [Effects.PreventDamage] shield. On resolution
 * the caster picks a source; the next time it would deal damage to the caster, the damage is still
 * dealt in full and a linked delayed trigger deals that much (`DynamicAmounts.preventedDamage`) to
 * that source's controller (`EffectTarget.ControllerOfTriggeringEntity`).
 */
val EyeForAnEye = card("Eye for an Eye") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "The next time a source of your choice would deal damage to you this turn, instead " +
        "that source deals that much damage to you and Eye for an Eye deals that much damage to that " +
        "source's controller."

    spell {
        effect = Effects.PreventDamage(
            sources = PreventionSourceFilter.Chosen(),
            stillDealt = true,
            onPrevented = Effects.DealDamage(
                amount = DynamicAmounts.preventedDamage(),
                target = EffectTarget.ControllerOfTriggeringEntity
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2933ca2a-097b-44f4-ae56-ad524d26fd06.jpg?1562902609"
    }
}
