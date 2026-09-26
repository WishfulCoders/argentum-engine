package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Targets

/**
 * Rakdos Joins Up
 * {3}{B}{R}
 * Legendary Enchantment
 *
 * When Rakdos Joins Up enters, return target creature card from your graveyard to the
 * battlefield with two additional +1/+1 counters on it.
 * Whenever a legendary creature you control dies, Rakdos Joins Up deals damage equal to
 * that creature's power to target opponent.
 *
 * Part of the OTJ "Joins Up" cycle of Legendary Enchantments. The ETB reanimation is the
 * standard Move(graveyard → battlefield).then(AddCounters) shape (cf. Evil Reawakened). The
 * dies trigger is `leavesBattlefield(... to GRAVEYARD)` over legendary creatures you control;
 * the damage uses [DynamicAmounts.triggeringPower], which resolves via last-known information
 * for the just-died creature (CR 603.10 / 112.7a).
 */
val RakdosJoinsUp = card("Rakdos Joins Up") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Enchantment"
    oracleText = "When Rakdos Joins Up enters, return target creature card from your graveyard to the battlefield with two additional +1/+1 counters on it.\n" +
        "Whenever a legendary creature you control dies, Rakdos Joins Up deals damage equal to that creature's power to target opponent."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(t, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, t)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.legendary().youControl()).dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.DealDamage(DynamicAmounts.triggeringPower(), opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Wylie Beckert"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7154dca-7e10-4c34-aa56-9f200c6277d1.jpg?1712356184"
    }
}
