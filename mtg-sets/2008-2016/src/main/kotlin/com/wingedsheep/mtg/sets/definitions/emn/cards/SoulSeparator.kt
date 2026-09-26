package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Soul Separator
 * {3}
 * Artifact
 * {5}, {T}, Sacrifice this artifact: Exile target creature card from your graveyard. Create a token
 * that's a copy of that card, except it's 1/1, it's a Spirit in addition to its other types, and it
 * has flying. Create a black Zombie creature token with power equal to that card's power and
 * toughness equal to that card's toughness.
 *
 * Modeled after Sauron, the Necromancer: exile the targeted graveyard creature card, then reference
 * "that card" (its [CardComponent] survives the move to exile) via [EffectTarget.ContextTarget].
 * - The copy token uses `overridePower`/`overrideToughness = 1` for the 1/1, `addedSubtypes` for
 *   "Spirit in addition to its other types", and `addedKeywords` for flying.
 * - The Zombie token's power/toughness are read from the exiled card's printed base stats via
 *   [DynamicAmount.EntityProperty] (a card outside the battlefield shows its printed characteristics).
 */
val SoulSeparator = card("Soul Separator") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{5}, {T}, Sacrifice this artifact: Exile target creature card from your graveyard. " +
        "Create a token that's a copy of that card, except it's 1/1, it's a Spirit in addition to " +
        "its other types, and it has flying. Create a black Zombie creature token with power equal " +
        "to that card's power and toughness equal to that card's toughness."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap, Costs.SacrificeSelf)
        val graveyardCreature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Exile(graveyardCreature) then
            Effects.CreateTokenCopyOfTarget(
                target = graveyardCreature,
                overridePower = 1,
                overrideToughness = 1,
                addedSubtypes = setOf(Subtype("Spirit")),
                addedKeywords = setOf(Keyword.FLYING)
            ) then
            Effects.CreateDynamicToken(
                dynamicPower = DynamicAmounts.powerOf(graveyardCreature),
                dynamicToughness = DynamicAmounts.toughnessOf(graveyardCreature),
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie")
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb9b1158-dfc4-4fe9-b970-338be8a99662.jpg?1782711810"
    }
}
