package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Moonlight Hunt
 * {1}{G}
 * Instant
 *
 * Choose target creature you don't control. Each creature you control that's a Wolf or a Werewolf
 * deals damage equal to its power to that creature.
 *
 * A one-sided gang-up: [Effects.ForEachInGroup] walks the Wolves and Werewolves you control at
 * resolution — an untargeted group, so a pack member entering or leaving between cast and
 * resolution is simply included or not — and each one deals damage *itself*
 * (`damageSource = EffectTarget.IterationEntity`, the iterated permanent) equal to its own power, read
 * per-iteration via [EffectTarget.IterationEntity] so lords and pump are picked up individually.
 *
 * Two details the wording forces:
 * - The damage sources matter, not just the total: deathtouch, lifelink, and "damage dealt by a
 *   Werewolf" triggers all key off the individual creature, which is why this can't collapse into a
 *   single summed damage event.
 * - Power is read as each creature deals its damage, and the whole loop is one resolution, so a
 *   creature that dies to an earlier pack member's damage… doesn't: damage isn't lethal until the
 *   state check after the spell finishes resolving, so every pack member deals its full damage.
 */
val MoonlightHunt = card("Moonlight Hunt") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose target creature you don't control. Each creature you control that's a " +
        "Wolf or a Werewolf deals damage equal to its power to that creature."

    spell {
        val creatureYouDonTControl = target(TargetFilter.CreatureOpponentControls)

        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                GameObjectFilter.Creature.withAnySubtype("Wolf", "Werewolf").youControl()
            ),
            effect = Effects.DealDamage(
                amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                target = creatureYouDonTControl,
                damageSource = EffectTarget.IterationEntity,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Joseph Meehan"
        flavorText = "Courage alone won't keep you alive."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9633603-a80f-448d-98d9-00064d379c26.jpg?1783937725"
    }
}
