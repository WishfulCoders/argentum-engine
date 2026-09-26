package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Oltec Matterweaver
 * {2}{W}
 * Creature — Human Artificer
 * 2/4
 *
 * Whenever you cast a creature spell, choose one —
 * • Create a 1/1 colorless Gnome artifact creature token.
 * • Create a token that's a copy of target artifact token you control.
 */
val OltecMatterweaver = card("Oltec Matterweaver") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 4
    oracleText = "Whenever you cast a creature spell, choose one —\n" +
        "• Create a 1/1 colorless Gnome artifact creature token.\n" +
        "• Create a token that's a copy of target artifact token you control."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                // 1/1 colorless (emptySet colors) Gnome artifact creature token.
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = emptySet(),
                    creatureTypes = setOf("Gnome"),
                    artifactToken = true,
                    imageUri = "https://cards.scryfall.io/normal/front/a/2/a257ae72-2a82-4861-a2fb-a0a09be00646.jpg?1712317852"
                ),
                "Create a 1/1 colorless Gnome artifact creature token"
            ),
            mode("Create a token that's a copy of target artifact token you control") {
                val artifact = target(TargetFilter(GameObjectFilter.Artifact.token().youControl()))
                effect = Effects.CreateTokenCopyOfTarget(target = artifact)
            }
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Villarrte"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4f2a818-9fd1-41db-967e-7d2c9b4e4c2f.jpg?1739804152"
    }
}
