package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Necropolis Fiend
 * {7}{B}{B}
 * Creature — Demon
 * 4/5
 * Delve
 * Flying
 * {X}, {T}, Exile X cards from your graveyard: Target creature gets -X/-X until end of turn.
 */
val NecropolisFiend = card("Necropolis Fiend") {
    manaCost = "{7}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 4
    toughness = 5
    oracleText = "Delve\nFlying\n{X}, {T}, Exile X cards from your graveyard: Target creature gets -X/-X until end of turn."

    keywords(Keyword.DELVE, Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap, Costs.ExileXFromGraveyard())
        val creature = target(TargetFilter.Creature)
        val negX = -DynamicAmounts.xValue()
        effect = Effects.ModifyStats(negX, negX, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/093426d2-29e0-49e4-b02a-a70cce3b25d5.jpg?1562782281"
    }
}
