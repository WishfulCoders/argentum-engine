package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deadly Precision
 * {B}
 * Sorcery
 * As an additional cost to cast this spell, pay {4} or sacrifice an artifact or creature.
 * Destroy target creature.
 *
 * The binary additional-cost fork (pay {4} or sacrifice an artifact or creature) is modeled
 * with [ModalEffect], mirroring Lash of the Balrog's "sacrifice a creature or pay {4}" pattern.
 * The spell is NOT modal in MTG terms (no "Choose one —" wording), so countsAsModalSpell = false.
 */
val DeadlyPrecision = card("Deadly Precision") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, pay {4} or sacrifice an artifact or creature.\nDestroy target creature."

    spell {
        effect = ModalEffect.chooseOne(
            // Pay {4}
            mode("Pay {4} — destroy target creature") {
                val creature = target(TargetFilter.Creature)
                additionalManaCost = "{4}"
                effect = Effects.Destroy(creature)
            },
            // Sacrifice an artifact or creature
            mode("Sacrifice an artifact or creature — destroy target creature") {
                val creature = target(TargetFilter.Creature)
                additionalCosts = listOf(
                    Costs.additional.SacrificePermanent(
                        filter = GameObjectFilter.Artifact.or(GameObjectFilter.Creature)
                    )
                )
                effect = Effects.Destroy(creature)
            },
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Yuhong Ding"
        flavorText = "Mai didn't always care, but she never missed."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4661daf6-d960-4278-8946-70948efaf99d.jpg?1764120656"
    }
}
