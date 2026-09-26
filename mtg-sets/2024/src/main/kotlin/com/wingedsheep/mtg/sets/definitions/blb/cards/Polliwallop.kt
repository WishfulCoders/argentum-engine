package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Polliwallop
 * {3}{G}
 * Instant
 *
 * Affinity for Frogs (This spell costs {1} less to cast for each Frog you control.)
 * Target creature you control deals damage equal to twice its power to target creature
 * you don't control.
 */
val Polliwallop = card("Polliwallop") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Affinity for Frogs (This spell costs {1} less to cast for each Frog you control.)\n" +
        "Target creature you control deals damage equal to twice its power to target creature you don't control."

    keywordAbility(KeywordAbility.AffinityForSubtype(Subtype.FROG))

    spell {
        val myCreature = target(TargetFilter.CreatureYouControl)
        val theirCreature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.powerOf(myCreature) * 2,
            target = theirCreature,
            damageSource = myCreature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Martin Wittfooth"
        flavorText = "\"We'll see who croaks first!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bc4963c-d90b-4588-bdb7-85956e42a623.jpg?1721426903"
        ruling("2024-07-26", "If either creature is an illegal target as Polliwallop resolves, the creature you control won't deal damage.")
    }
}
