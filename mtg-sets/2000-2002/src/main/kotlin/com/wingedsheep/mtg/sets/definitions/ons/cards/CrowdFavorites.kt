package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crowd Favorites
 * {6}{W}
 * Creature — Human Soldier
 * 4/4
 * {3}{W}: Tap target creature.
 * {3}{W}: Crowd Favorites gets +0/+5 until end of turn.
 */
val CrowdFavorites = card("Crowd Favorites") {
    manaCost = "{6}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "{3}{W}: Tap target creature.\n{3}{W}: Crowd Favorites gets +0/+5 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(
            target = t
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        effect = Effects.ModifyStats(
            power = 0,
            toughness = 5,
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Roger Raupp"
        flavorText = "\"The rabble likes them. Make sure they win, then book them for tomorrow.\" —Cabal Patriarch"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1038436d-aea5-4508-8b37-c2cfa32c2771.jpg?1593017609"
    }
}
