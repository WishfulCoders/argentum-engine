package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hivespine Wolverine
 * {3}{G}{G}
 * Creature — Elemental Wolverine
 * 5/4
 *
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on target creature you control.
 * • This creature fights target creature token.
 * • Destroy target artifact or enchantment.
 */
val HivespineWolverine = card("Hivespine Wolverine") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental Wolverine"
    oracleText = "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on target creature you control.\n" +
        "• This creature fights target creature token.\n" +
        "• Destroy target artifact or enchantment."
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            // Mode 1: Put a +1/+1 counter on target creature you control
            mode("Put a +1/+1 counter on target creature you control") {
                val creatureYouControl = target(TargetFilter.CreatureYouControl)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creatureYouControl)
            },
            // Mode 2: This creature fights target creature token
            mode("This creature fights target creature token") {
                val creature = target(
                    TargetFilter(
                        GameObjectFilter(cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsToken))
                    ),
                )
                effect = Effects.Fight(EffectTarget.Self, creature)
            },
            // Mode 3: Destroy target artifact or enchantment
            mode("Destroy target artifact or enchantment") {
                val artifactOrEnchantment = target(TargetFilter.ArtifactOrEnchantment)
                effect = Effects.Destroy(artifactOrEnchantment)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/821970a3-a291-4fe9-bb13-dfc54f9c3caf.jpg?1721426835"
    }
}
