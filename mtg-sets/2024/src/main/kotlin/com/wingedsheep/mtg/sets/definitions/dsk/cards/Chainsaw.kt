package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Chainsaw
 * {1}{R}
 * Artifact — Equipment
 * When this Equipment enters, it deals 3 damage to up to one target creature.
 * Whenever one or more creatures die, put a rev counter on this Equipment.
 * Equipped creature gets +X/+0, where X is the number of rev counters on this Equipment.
 * Equip {3}
 *
 * The "whenever one or more creatures die" trigger is the batched death shape
 * (`Triggers.oneOrMore(filter.anyController()).die()`): it fires at most once per death batch regardless of how many
 * creatures died simultaneously and regardless of who controlled them (CR 603.3b), so a board wipe
 * adds exactly one rev counter, not one per creature.
 *
 * Rev counters are passive storage counters (no inherent rule); the +X/+0 static reads the count off
 * the Equipment (the source) via [DynamicAmounts.countersOnSelf] and applies it to the equipped
 * creature ([Filters.EquippedCreature]).
 */
val Chainsaw = card("Chainsaw") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, it deals 3 damage to up to one target creature.\n" +
        "Whenever one or more creatures die, put a rev counter on this Equipment.\n" +
        "Equipped creature gets +X/+0, where X is the number of rev counters on this Equipment.\n" +
        "Equip {3}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature, optional = true)
        effect = Effects.DealDamage(3, t)
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.anyController()).die()
        effect = Effects.AddCounters(CounterType.REV, 1, EffectTarget.Self)
    }

    staticAbility {
        val revCount = DynamicAmounts.countersOnSelf(CounterType.REV)
        ability = GrantDynamicStats(
            filter = Filters.EquippedCreature,
            powerBonus = revCount,
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "J.P. Targete"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54e0c2cd-fa5f-427d-8e15-6066f002a8e3.jpg?1726286325"
    }
}
