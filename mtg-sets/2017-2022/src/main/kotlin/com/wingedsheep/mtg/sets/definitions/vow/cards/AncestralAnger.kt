package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ancestral Anger
 * {R}
 * Sorcery
 * Target creature gains trample and gets +X/+0 until end of turn, where X is 1 plus the
 * number of cards named Ancestral Anger in your graveyard.
 * Draw a card.
 *
 * X resolves at resolution: `1 + (cards named "Ancestral Anger" in your graveyard)`. The
 * graveyard count is `DynamicAmount.Count` over `CardPredicate.NameEquals`, offset by one via
 * `DynamicAmount.Add`. Each copy that has been cast and gone to the graveyard pumps the next.
 */
val AncestralAnger = card("Ancestral Anger") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature gains trample and gets +X/+0 until end of turn, where X is 1 " +
        "plus the number of cards named Ancestral Anger in your graveyard.\nDraw a card."

    spell {
        val t = target(TargetFilter.Creature)
        val pump = 1 + DynamicAmounts.count(
            Player.You,
            Zone.GRAVEYARD,
            GameObjectFilter(
                cardPredicates = listOf(CardPredicate.NameEquals("Ancestral Anger")),
            ),
        )
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, t) then
            Effects.ModifyStats(pump, DynamicAmounts.fixed(0), t) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Randy Vargas"
        flavorText = "\"One vampire for every family member I've lost.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dee47ab-d603-4346-97f4-a25dc3f47765.jpg?1643590713"
    }
}
