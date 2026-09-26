package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Plow Through
 * {G}
 * Sorcery
 *
 * Choose one —
 * • Target creature you control fights target creature an opponent controls.
 * • Destroy target Vehicle.
 *
 * A true "Choose one" modal spell ([ModalEffect.chooseOne], counts as modal). Mode 1 is a
 * standard two-target fight; mode 2 destroys a Vehicle (an artifact with the Vehicle subtype).
 */
val PlowThrough = card("Plow Through") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Target creature you control fights target creature an opponent controls. " +
        "(Each deals damage equal to its power to the other.)\n" +
        "• Destroy target Vehicle."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: fight
            mode("Target creature you control fights target creature an opponent controls") {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.Fight(creatureYouControl, creatureOpponentControls)
            },
            // Mode 2: destroy target Vehicle
            mode("Destroy target Vehicle") {
                val artifact = target(TargetFilter(GameObjectFilter.Artifact.withSubtype(Subtype.VEHICLE)))
                effect = Effects.Destroy(artifact)
            },
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Brian Valeza"
        flavorText = "Like most goblins, Rocketeers value nothing more than a good old-fashioned near-death experience."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a311d4b3-ab2a-43c2-8480-6c5daac41178.jpg?1782687824"
    }
}
