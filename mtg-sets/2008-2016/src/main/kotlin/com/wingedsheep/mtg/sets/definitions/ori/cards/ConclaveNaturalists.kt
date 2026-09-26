package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Conclave Naturalists
 * {4}{G}
 * Creature — Dryad
 * 4/4
 *
 * When this creature enters, you may destroy target artifact or enchantment.
 */
val ConclaveNaturalists = card("Conclave Naturalists") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    oracleText = "When this creature enters, you may destroy target artifact or enchantment."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val t = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Howard Lyon"
        flavorText = "\"Your swords and wards have no power here.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3759fc28-9adb-41ed-851c-566a3a424e09.jpg"
    }
}
