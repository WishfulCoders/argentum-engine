package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Chandra's Ignition
 * {3}{R}{R}
 * Sorcery
 * Target creature you control deals damage equal to its power to each other creature and each
 * opponent.
 *
 * Betrayal at the Vault's "deals damage equal to its power" with the creature as the damage source
 * (so a white creature hits a creature with protection from red, ruling), over every other creature
 * and then each opponent, its power read as the spell resolves.
 */
val ChandrasIgnition = card("Chandra's Ignition") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to each other creature and each opponent."

    spell {
        target(TargetFilter.CreatureYouControl)
        val power = DynamicAmount.EntityProperty(EffectTarget.ContextTarget(0), EntityNumericProperty.Power)
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures.otherThanTarget(),
            Effects.DealDamage(power, EffectTarget.Self, damageSource = EffectTarget.ContextTarget(0))
        ).then(
            Effects.DealDamage(power, EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Eric Deschamps"
        flavorText = "In the moment before her execution, she realized what it meant to be a pyromancer, to be alive, to be Chandra."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d4c90de-49aa-43ed-a18a-f7f96268e5eb.jpg?1783938332"
        ruling("2015-06-22", "The creature is the source of the damage, not Chandra's Ignition. For example, Chandra's Ignition can have a white creature deal damage to a creature with protection from red.")
        ruling("2015-06-22", "Use the power of the target creature as Chandra's Ignition resolves to determine how much damage it deals to each other creature and each opponent.")
        ruling("2015-06-22", "If the creature becomes an illegal target by the time Chandra's Ignition tries to resolve (perhaps because another player controls it or it's left the battlefield), Chandra's Ignition won't resolve and none of its effects will happen. No damage will be dealt.")
    }
}
