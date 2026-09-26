package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lash of the Balrog
 * {B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a creature or pay {4}.
 * Destroy target creature.
 *
 * The binary additional-cost fork (sacrifice a creature or pay {4}) is modeled with
 * [ModalEffect]; the spell is NOT modal in MTG terms (no "Choose one —" wording), so
 * countsAsModalSpell = false.
 */
val LashOfTheBalrog = card("Lash of the Balrog") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature or pay {4}.\nDestroy target creature."

    spell {
        effect = ModalEffect.chooseOne(
            // Sacrifice a creature
            mode("Sacrifice a creature — destroy target creature") {
                val creature = target(TargetFilter.Creature)
                additionalCosts = listOf(
                    Costs.additional.SacrificePermanent(filter = GameObjectFilter.Creature)
                )
                effect = Effects.Destroy(creature)
            },
            // Pay {4}
            mode("Pay {4} — destroy target creature") {
                val creature = target(TargetFilter.Creature)
                additionalManaCost = "{4}"
                effect = Effects.Destroy(creature)
            },
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Antonio José Manzanedo"
        flavorText = "Even as the Balrog fell, it swung its whip, and the thongs lashed and curled about Gandalf's knees, dragging him to the brink of the abyss. \"Fly, you fools!\" he cried, and was gone."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/812fee97-e145-4458-b495-bc6ad227335b.jpg?1686968542"
    }
}
