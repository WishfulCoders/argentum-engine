package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Pest Infestation
 * {X}{X}{G}
 * Sorcery
 * Destroy up to X target artifacts and/or enchantments. Create twice X 1/1 black and green Pest
 * creature tokens with "When this token dies, you gain 1 life."
 *
 * "Up to X target" is an optional requirement whose maximum is the chosen X
 * ([TargetPermanent.dynamicMaxCount]); each chosen target is destroyed through
 * [ForEachTargetEffect]. The Pests are twice X regardless of how many permanents were destroyed
 * (ruling); if targets were chosen and all are illegal on resolution, the spell doesn't resolve and
 * makes no Pests — the engine's usual all-targets-illegal fizzle.
 */
val PestInfestation = card("Pest Infestation") {
    manaCost = "{X}{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy up to X target artifacts and/or enchantments. Create twice X 1/1 black and green Pest creature tokens with \"When this token dies, you gain 1 life.\""

    spell {
        target = TargetObject(
            optional = true,
            filter = TargetFilter.ArtifactOrEnchantment,
            dynamicMaxCount = DynamicAmount.XValue
        )
        effect = Effects.Composite(listOf(
            ForEachTargetEffect(listOf(Effects.Destroy(EffectTarget.ContextTarget(0)))),
            CreateTokenEffect(
                count = DynamicAmount.Multiply(DynamicAmount.XValue, 2),
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK, Color.GREEN),
                creatureTypes = setOf("Pest"),
                triggeredAbilities = listOf(
                    TriggeredAbility.create(
                        trigger = Triggers.self.dies().event,
                        binding = Triggers.self.dies().binding,
                        effect = Effects.GainLife(1)
                    )
                ),
                imageUri = "https://cards.scryfall.io/normal/front/d/0/d0ddbe3e-4a66-494d-9304-7471232549bf.jpg?1783927190"
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Brian Valeza"
        flavorText = "A thousand years of history undone by one juvenile prank."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/4720b4f2-e6af-4223-9250-a0ed21ed5693.jpg?1783927588"
        ruling("2021-04-16", "The number of Pests created is equal to twice the chosen value of X, no matter how many artifacts and enchantments were destroyed.")
        ruling("2021-04-16", "If any artifacts or enchantments were chosen as targets, and all of them are illegal targets as Pest Infestation tries to resolve, it won't resolve and none of its effects will happen. No Pests will be created.")
    }
}
