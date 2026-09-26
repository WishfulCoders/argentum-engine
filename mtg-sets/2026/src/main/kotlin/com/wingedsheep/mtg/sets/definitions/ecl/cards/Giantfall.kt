package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Giantfall
 * {1}{R}
 * Instant
 *
 * Choose one —
 * • Target creature you control deals damage equal to its power to target creature an opponent controls.
 * • Destroy target artifact.
 */
val Giantfall = card("Giantfall") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Target creature you control deals damage equal to its power to target creature an opponent controls.\n• Destroy target artifact."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Target creature you control deals damage equal to its power to target creature an opponent controls") {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.DealDamage(
                    amount = DynamicAmounts.powerOf(creatureYouControl),
                    target = creatureOpponentControls,
                    damageSource = creatureYouControl
                )
            },
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Drew Baker"
        flavorText = "Bump went the giant. Gone went the hamlet."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1ac52728-adb3-4220-8392-73f7bd379ab4.jpg?1767957191"
    }
}
