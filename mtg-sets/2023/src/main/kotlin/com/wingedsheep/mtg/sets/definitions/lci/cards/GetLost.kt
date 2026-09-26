package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Get Lost — LCI #14
 * {1}{W} Instant — Rare
 *
 * "Destroy target creature, enchantment, or planeswalker. Its controller creates two Map tokens.
 *  (They're artifacts with "{1}, {T}, Sacrifice this token: Target creature you control explores.
 *  Activate only as a sorcery.")"
 *
 * Target filter: There is no pre-built TargetFilter for "creature, enchantment, or planeswalker",
 * so we construct one inline using CardPredicate.Or. The Map tokens go to the destroyed
 * permanent's controller rather than the caster via the [EffectTarget.TargetController] controller
 * override.
 */
private val creatureEnchantmentOrPlaneswalker = TargetFilter(
    GameObjectFilter(
        cardPredicates = listOf(
            CardPredicate.Or(
                listOf(CardPredicate.IsCreature, CardPredicate.IsEnchantment, CardPredicate.IsPlaneswalker)
            )
        )
    )
)

val GetLost = card("Get Lost") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target creature, enchantment, or planeswalker. Its controller creates two Map tokens. " +
        "(They're artifacts with \"{1}, {T}, Sacrifice this token: Target creature you control explores. " +
        "Activate only as a sorcery.\")"

    spell {
        val t = target(creatureEnchantmentOrPlaneswalker)
        effect = Effects.Destroy(t) then Effects.CreateMapToken(2, controller = EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/522aa72b-2b8c-484c-872b-f082101cee35.jpg?1782694599"
    }
}
