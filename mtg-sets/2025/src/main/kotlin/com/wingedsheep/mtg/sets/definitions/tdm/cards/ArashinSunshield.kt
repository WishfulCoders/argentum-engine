package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Arashin Sunshield
 * {3}{W}
 * Creature — Human Warrior
 * 3/4
 *
 * When this creature enters, exile up to two target cards from a single graveyard.
 * {W}, {T}: Tap target creature.
 */
val ArashinSunshield = card("Arashin Sunshield") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, exile up to two target cards from a single graveyard.\n" +
        "{W}, {T}: Tap target creature."

    // ETB: exile up to two target cards, both from the same graveyard (sameOwner).
    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.CardInGraveyard, count = 2, optional = true, sameOwner = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE)
        )
    }

    // {W}, {T}: Tap target creature.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(target = t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Inkognit"
        flavorText = "\"The best way to protect the citizens is to defend Arashin, not hunt dragonstorms across Tarkir.\"\n—Sadesh of House Mevak"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd7102d8-90b3-45a1-b66d-dcca469b1fb6.jpg?1743697519"
    }
}
