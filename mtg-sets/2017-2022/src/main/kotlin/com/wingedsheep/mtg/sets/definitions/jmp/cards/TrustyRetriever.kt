package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trusty Retriever
 * {3}{W}
 * Creature — Dog
 * 2/3
 *
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on this creature.
 * • Return target artifact or enchantment card from your graveyard to your hand.
 */
val TrustyRetriever = card("Trusty Retriever") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog"
    oracleText = "When this creature enters, choose one —\n• Put a +1/+1 counter on this creature.\n• Return target artifact or enchantment card from your graveyard to your hand."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                "Put a +1/+1 counter on this creature",
            ),
            mode("Return target artifact or enchantment card from your graveyard to your hand") {
                val artifactOrEnchantment = target(
                    TargetFilter(GameObjectFilter.ArtifactOrEnchantment.ownedByYou(), zone = Zone.GRAVEYARD),
                )
                effect = Effects.ReturnToHand(artifactOrEnchantment)
            },
        )
        description = "When this creature enters, choose one — Put a +1/+1 counter on this creature; " +
            "or return target artifact or enchantment card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Ralph Horsley"
        flavorText = "Wipe away the slobber and it's as good as new, give or take a few bite marks."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab4eb490-acd0-4162-8e8a-7e7ff003d0f3.jpg?1783930507"
    }
}
