package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Trap Digger
 * {3}{W}
 * Creature — Human Soldier
 * 1/3
 * {2}{W}, {T}: Put a trap counter on target land you control.
 * Sacrifice a land with a trap counter on it: Trap Digger deals 3 damage to target attacking creature without flying.
 */
val TrapDigger = card("Trap Digger") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 3
    oracleText = "{2}{W}, {T}: Put a trap counter on target land you control.\nSacrifice a land with a trap counter on it: Trap Digger deals 3 damage to target attacking creature without flying."

    // {2}{W}, {T}: Put a trap counter on target land you control.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap)
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.AddCounters(CounterType.TRAP, 1, land)
    }

    // Sacrifice a land with a trap counter on it: Trap Digger deals 3 damage to target attacking creature without flying.
    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Land.withCounter(CounterType.TRAP))
        val creature = target(TargetFilter(GameObjectFilter.Creature.attacking().withoutKeyword(Keyword.FLYING)))
        effect = Effects.DealDamage(3, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05cd76bf-db08-45f8-b3ae-501bcca6df3c.jpg?1562524971"
    }
}
