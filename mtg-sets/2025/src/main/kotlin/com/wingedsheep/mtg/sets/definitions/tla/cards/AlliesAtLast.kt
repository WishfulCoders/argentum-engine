package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Allies at Last
 * {2}{G}
 * Instant
 *
 * Affinity for Allies (This spell costs {1} less to cast for each Ally you control.)
 * Up to two target creatures you control each deal damage equal to their power to target
 * creature an opponent controls.
 *
 * Two target slots: the single opponent's creature (declared first, so it stays addressable as
 * [EffectTarget.ContextTarget] index 0 across the per-attacker loop) and up to two creatures you
 * control. At resolution we gather the chosen targets, filter to the creatures you control
 * (excludes the victim), then have each deal damage equal to its own power — read per-iteration via
 * [EffectTarget.IterationEntity] — to the opponent's creature.
 */
val AlliesAtLast = card("Allies at Last") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Affinity for Allies (This spell costs {1} less to cast for each Ally you control.)\n" +
        "Up to two target creatures you control each deal damage equal to their power to target " +
        "creature an opponent controls."

    keywordAbility(KeywordAbility.AffinityForSubtype(Subtype.ALLY))

    spell {
        // Declared first so the victim is a stable target across the per-creature loop.
        val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls)
        targets(TargetFilter(GameObjectFilter.Creature.youControl()), count = 2, minCount = 0)

        effect = Effects.Pipeline {
            // Gather every chosen target, then keep only the creatures you control (the victim is
            // an opponent's creature, so it drops out).
            val allTargets = gather(CardSource.ChosenTargets)
            val allies = filter(allTargets, GameObjectFilter.Creature.youControl())
            // Each chosen creature deals damage equal to its power to the opponent's creature.
            run(Effects.ForEachInCollection(
                collection = allies,
                effect = Effects.DealDamage(
                    amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                    target = creatureOpponentControls,
                    damageSource = EffectTarget.IterationEntity,
                ),
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Evan Shipard"
        flavorText = "Opposing elements working together in harmony."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11a77897-2aba-4a9b-bbe6-1768ca9f12cb.jpg?1764121122"
    }
}
