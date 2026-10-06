package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * March of Otherworldly Light
 * {X}{W}
 * Instant
 *
 * As an additional cost to cast this spell, you may exile any number of white cards from your
 * hand. This spell costs {2} less to cast for each card exiled this way.
 * Exile target artifact, creature, or enchantment with mana value X or less.
 *
 * The exiled cards reduce the *total* cost, X included (CR 601.2f): with X = 6, two exiled white
 * cards make it {2}{W}. Nothing reduces the {W}. X itself — and so the target's mana-value cap and
 * the spell's mana value on the stack — is whatever was announced.
 */
val MarchOfOtherworldlyLight = card("March of Otherworldly Light") {
    manaCost = "{X}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may exile any number of white cards from your " +
        "hand. This spell costs {2} less to cast for each card exiled this way.\n" +
        "Exile target artifact, creature, or enchantment with mana value X or less."

    additionalCost(
        Costs.additional.ExileCardsForCostReduction(
            filter = GameObjectFilter.Any.withColor(Color.WHITE),
            costReductionPerCard = 2,
        )
    )

    spell {
        val permanent = target(TargetFilter.ArtifactCreatureOrEnchantment.manaValueAtMostX())
        effect = Effects.Exile(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/553fb946-2706-475b-89f9-e4355ec9ea2b.jpg?1783923916"
        ruling("2022-02-18", "You may exile more cards than necessary for March of Otherworldly Light's first ability, but you can't reduce the mana it costs to less than {W} this way.")
        ruling("2022-02-18", "For example, say you wanted to choose a target with mana value 6. You need X to be at least 6. You could choose to cast March of Otherworldly Light by paying {6}{W}. You could also exile white cards from your hand to reduce that cost by {2} for each one. If you had a reason to, you could even choose a greater value for X.")
        ruling("2022-02-18", "The mana value of March of Otherworldly Light while it's on the stack is the value chosen for X plus 1, no matter how much mana you actually paid to cast it.")
    }
}
