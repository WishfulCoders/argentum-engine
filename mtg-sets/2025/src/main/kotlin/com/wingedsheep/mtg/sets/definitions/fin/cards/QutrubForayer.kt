package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Qutrub Forayer
 * {2}{B}
 * Creature — Zombie Horror
 * 3/2
 *
 * When this creature enters, choose one —
 * • Destroy target creature that was dealt damage this turn.
 * • Exile up to two target cards from a single graveyard.
 *
 * Modal ETB trigger. The "single graveyard" constraint on mode two is the cross-target
 * [TargetObject.sameOwner] flag (same modeling as Arashin Sunshield); the dealt-damage mode
 * gates its target with [TargetFilter.wasDealtDamageThisTurn].
 */
val QutrubForayer = card("Qutrub Forayer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Horror"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, choose one —\n" +
        "• Destroy target creature that was dealt damage this turn.\n" +
        "• Exile up to two target cards from a single graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            // Mode 1: Destroy target creature that was dealt damage this turn.
            mode("Destroy target creature that was dealt damage this turn") {
                val creature = target(TargetFilter.Creature.wasDealtDamageThisTurn())
                effect = Effects.Destroy(creature)
            },
            // Mode 2: Exile up to two target cards from a single graveyard.
            Mode(
                effect = Effects.ForEachTarget(
                    Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE)
                ),
                targetRequirements = listOf(
                    TargetObject(
                        count = 2,
                        optional = true,
                        filter = TargetFilter.CardInGraveyard,
                        sameOwner = true,
                    )
                ),
                description = "Exile up to two target cards from a single graveyard"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Lordigan"
        flavorText = "They willfully attain an undead existence to serve their foul mistresses for eternity."
        imageUri = "https://cards.scryfall.io/normal/front/7/4/7475ecf6-23f5-45af-9ef0-ac7923bbc9cb.jpg?1748706179"
    }
}
