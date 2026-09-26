package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Step

/**
 * Lifecrafter's Bestiary
 * {3}
 * Artifact
 *
 * At the beginning of your upkeep, scry 1.
 * Whenever you cast a creature spell, you may pay {G}. If you do, draw a card.
 *
 * The second ability is a plain "you may pay … if you do" gate ([Effects.MayPay]), not a
 * reflexive trigger — nothing is targeted, so the payment and the draw happen in one resolution.
 */
val LifecraftersBestiary = card("Lifecrafter's Bestiary") {
    manaCost = "{3}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, scry 1.\n" +
        "Whenever you cast a creature spell, you may pay {G}. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Scry(1)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        effect = Effects.MayPay(ManaCost.parse("{G}"), Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Izzy"
        flavorText = "\"Inspiration is found by looking outward.\"\n—Oviya Pashiri, sage lifecrafter"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/7439a855-4041-4d14-8edf-6741a734e55d.jpg"
    }
}
