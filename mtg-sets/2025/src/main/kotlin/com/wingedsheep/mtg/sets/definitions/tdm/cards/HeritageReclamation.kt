package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Heritage Reclamation
 * {1}{G}
 * Instant
 *
 * Choose one —
 * • Destroy target artifact.
 * • Destroy target enchantment.
 * • Exile up to one target card from a graveyard. Draw a card.
 */
val HeritageReclamation = card("Heritage Reclamation") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Destroy target artifact.\n" +
        "• Destroy target enchantment.\n" +
        "• Exile up to one target card from a graveyard. Draw a card."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            },
            mode("Destroy target enchantment") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            },
            // "Exile up to one target card from a graveyard. Draw a card."
            // The exile target is optional (up to one); the draw happens unconditionally.
            mode("Exile up to one target card from a graveyard. Draw a card.") {
                val targetedObject = target(TargetFilter.CardInGraveyard, optional = true)
                effect = Effects.Exile(targetedObject) then Effects.DrawCards(1)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Konstantin Porubov"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f8fee37-a050-4329-8b10-46d150e7a95e.jpg?1743204546"
    }
}
