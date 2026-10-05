package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost

/**
 * Mine Collapse
 * {3}{R} — Instant (Common) — Modern Horizons 2 #135
 * Artist: Bud Cook
 *
 * If it's your turn, you may sacrifice a Mountain rather than pay this spell's mana cost.
 * Mine Collapse deals 5 damage to target creature or planeswalker.
 *
 * The alternative cost is Fireblast's shape (a {0} [SelfAlternativeCost] whose additional cost
 * is the sacrifice), gated by [Conditions.IsYourTurn] like the Force cycle's off-turn gate.
 */
val MineCollapse = card("Mine Collapse") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "If it's your turn, you may sacrifice a Mountain rather than pay this spell's mana cost.\n" +
        "Mine Collapse deals 5 damage to target creature or planeswalker."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.SacrificePermanent(Filters.MountainCard)
        ),
        condition = Conditions.IsYourTurn
    )

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(5, victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Bud Cook"
        flavorText = "\"Good ol' rock,\" muttered Rhirhok as the shaking began. \"Nice ol' rock . . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56e2e8b5-660d-4469-a4fe-2367dfadb709.jpg?1783926842"
    }
}
