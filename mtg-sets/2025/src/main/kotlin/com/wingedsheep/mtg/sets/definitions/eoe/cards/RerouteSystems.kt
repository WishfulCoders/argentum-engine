package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reroute Systems
 * {W}
 * Instant
 * Choose one —
 * • Target artifact or creature gains indestructible until end of turn. (Damage and effects that say "destroy" don't destroy it.)
 * • Reroute Systems deals 2 damage to target tapped creature.
 */
val RerouteSystems = card("Reroute Systems") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Target artifact or creature gains indestructible until end of turn. (Damage and effects that say \"destroy\" don't destroy it.)\n• Reroute Systems deals 2 damage to target tapped creature."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: Target artifact or creature gains indestructible until end of turn
            mode("Target artifact or creature gains indestructible until end of turn") {
                val artifact = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature))
                effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, artifact)
            },
            // Mode 2: Reroute Systems deals 2 damage to target tapped creature
            mode("Reroute Systems deals 2 damage to target tapped creature") {
                val tappedCreature = target(TargetFilter.TappedCreature)
                effect = Effects.DealDamage(2, tappedCreature)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bbdba38-2b99-4226-98e3-6d2580345d6d.jpg?1752946673"
    }
}
