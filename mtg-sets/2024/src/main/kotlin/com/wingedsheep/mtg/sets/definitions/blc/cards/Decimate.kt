package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Decimate {2}{R}{G}
 * Sorcery
 *
 * Destroy target artifact, target creature, target enchantment, and target land.
 * (You can't cast this spell unless you have legal choices for all its targets.)
 */
val Decimate = card("Decimate") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact, target creature, target enchantment, and target land. " +
        "(You can't cast this spell unless you have legal choices for all its targets.)"

    spell {
        val artifact = target(TargetFilter.Artifact)
        val creature = target(TargetFilter.Creature)
        val enchantment = target(TargetFilter.Enchantment)
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(artifact) then
            Effects.Destroy(creature) then
            Effects.Destroy(enchantment) then
            Effects.Destroy(land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "251"
        artist = "Zoltan Boros"
        flavorText = "Anarchy comes in many forms: social, individual, Gruul . . ."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed38da33-c230-4eec-b7c7-3b0c5cdf727a.jpg?1721429459"
    }
}
