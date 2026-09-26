package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Digsite Conservator
 * {2}
 * Artifact Creature — Gnome
 * 2/1
 * Sacrifice this creature: Exile up to four target cards from a single graveyard. Activate only as a sorcery.
 * When this creature dies, you may pay {4}. If you do, discover 4.
 */
val DigsiteConservator = card("Digsite Conservator") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Gnome"
    power = 2
    toughness = 1
    oracleText = "Sacrifice this creature: Exile up to four target cards from a single graveyard. Activate only as a sorcery.\nWhen this creature dies, you may pay {4}. If you do, discover 4."

    activatedAbility {
        cost = Costs.SacrificeSelf
        targets(TargetFilter.CardInGraveyard, count = 4, optional = true, sameOwner = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE)
        )
        timing = TimingRule.SorcerySpeed
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{4}"),
            then = Effects.Discover(4)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "Racrufi"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfa3cd5e-b727-479d-9a77-9f320b92f3f2.jpg?1782694409"
    }
}
