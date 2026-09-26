package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Pyramids
 * {6}
 * Artifact
 *
 * {2}: Choose one —
 *   • Destroy target Aura attached to a land.
 *   • The next time target land would be destroyed this turn, remove all damage
 *     marked on it instead.
 */
val Pyramids = card("Pyramids") {
    manaCost = "{6}"
    typeLine = "Artifact"
    oracleText = "{2}: Choose one —\n" +
        "• Destroy target Aura attached to a land.\n" +
        "• The next time target land would be destroyed this turn, " +
        "remove all damage marked on it instead."

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = ModalEffect.chooseOne(
            mode("Destroy target Aura attached to a land") {
                val enchantment = target(
                    TargetFilter(
                        GameObjectFilter.Enchantment.withSubtype("Aura").copy(
                            statePredicates = listOf(
                                StatePredicate.AttachedToCardType(CardType.LAND)
                            )
                        )
                    ),
                )
                effect = Effects.Destroy(enchantment)
            },
            mode("The next time target land would be destroyed this turn, " +
                "remove all damage marked on it instead") {
                val land = target(TargetFilter.Land)
                effect = Effects.RemoveDamageShield(land)
            }
        )
        description = "{2}: Destroy target Aura attached to a land, or shield " +
            "target land from the next destruction this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2e9decf-47b7-44e0-b380-8055b6011021.jpg?1562934392"
    }
}
