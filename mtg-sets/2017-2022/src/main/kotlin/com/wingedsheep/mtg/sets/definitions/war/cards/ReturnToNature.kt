package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Return to Nature
 * {1}{G}
 * Instant
 *
 * Choose one —
 * • Destroy target artifact.
 * • Destroy target enchantment.
 * • Exile target card from a graveyard.
 *
 * Three modes, each with its own single target; a mode's targets belong to the mode, so each
 * mode's effect reads its own chosen object.
 */
val ReturnToNature = card("Return to Nature") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Destroy target artifact.\n" +
        "• Destroy target enchantment.\n" +
        "• Exile target card from a graveyard."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Destroy target artifact.") {
                effect = Effects.Destroy(target(TargetFilter.Artifact))
            },
            mode("Destroy target enchantment.") {
                effect = Effects.Destroy(target(TargetFilter.Enchantment))
            },
            mode("Exile target card from a graveyard.") {
                effect = Effects.Exile(target(TargetFilter.CardInGraveyard))
            },
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Alayna Danner"
        flavorText = "\"Yes, nature is stronger. You don't see little buildings sprouting on trees.\"\n—Emmara"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/085e3129-591b-46ec-ac8b-cff428927c01.jpg"
    }
}
