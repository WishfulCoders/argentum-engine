package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bone Shards
 * {B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a creature or discard a card.
 * Destroy target creature or planeswalker.
 *
 * The either-or additional cost is the cost-vs-cost [Costs.additional.Choice] (Souls of the Lost's
 * shape): exactly one branch is paid, once, and the spell can't be cast with neither.
 */
val BoneShards = card("Bone Shards") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature or discard a card.\n" +
        "Destroy target creature or planeswalker."

    additionalCost(
        Costs.additional.Choice(
            Costs.additional.SacrificePermanent(GameObjectFilter.Creature),
            Costs.additional.DiscardCards(),
        )
    )

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Tommy Arnold"
        flavorText = "In the Kathari dumping grounds known as the Boneheaps, its not hard to improvise an offense."
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ee98955-4c47-4d45-9377-608dfa755337.jpg?1783926865"
        ruling("2021-06-18", "You must sacrifice exactly one creature or discard exactly one card to cast this spell; you can't cast it without sacrificing a creature or discarding a card, and you can't sacrifice additional creatures or discard additional cards.")
    }
}
