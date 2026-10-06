package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.reconfigure
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lion Sash
 * {1}{W}
 * Artifact Creature — Equipment Cat
 * 1/1
 *
 * {W}: Exile target card from a graveyard. If it was a permanent card, put a +1/+1 counter on
 * this permanent.
 * Equipped creature gets +1/+1 for each +1/+1 counter on this Equipment.
 * Reconfigure {2}
 *
 * Reconfigure (CR 702.151) attaches it to another creature you control at sorcery speed, and
 * while attached it isn't a creature (CR 702.151b) — its +1/+1 counters stay on it and pump the
 * equipped creature instead. Unattached, it is a creature that wears its own counters.
 */
val LionSash = card("Lion Sash") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Equipment Cat"
    power = 1
    toughness = 1
    oracleText = "{W}: Exile target card from a graveyard. If it was a permanent card, put a +1/+1 counter " +
        "on this permanent.\n" +
        "Equipped creature gets +1/+1 for each +1/+1 counter on this Equipment.\n" +
        "Reconfigure {2} ({2}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    activatedAbility {
        cost = Costs.Mana("{W}")
        val exiled = target(TargetFilter.CardInGraveyard)
        effect = Effects.Exile(exiled) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Permanent, exiled),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            )
    }

    staticAbility {
        val counters = DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE)
        ability = GrantDynamicStats(
            filter = Filters.EquippedCreature,
            powerBonus = counters,
            toughnessBonus = counters,
        )
    }

    reconfigure("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e1766e9-2fa7-4446-a255-7beea1467ece.jpg?1783923917"
        ruling("2022-02-18", "Reconfigure represents two activated abilities. Reconfigure [cost] means \"[Cost]: Attach this permanent to another target creature you control. Activate only as a sorcery,\" and \"[Cost]: Unattach this permanent. Activate only if this permanent is attached to a creature and only as a sorcery.\"")
        ruling("2022-02-18", "Attaching an Equipment with reconfigure to a creature causes that Equipment to stop being a creature until it becomes unattached. It also loses any creature subtypes it had.")
        ruling("2022-02-18", "An Equipment doesn't become tapped when the permanent it's attached to becomes tapped. For example, if you attack with a creature that is equipped with Acquisition Octopus, then use reconfigure to unattach Acquisition Octopus after combat, the Octopus will be untapped and could be used to block during your opponent's turn.")
        ruling("2022-02-18", "If an Equipment with reconfigure somehow loses its abilities while it is attached, the effect causing it to not be a creature continues to apply until it becomes unattached.")
        ruling("2022-02-18", "As soon as an Equipment creature with reconfigure stops being a creature, any Equipment and Auras with enchant creature abilities become unattached. Auras that can enchant an Equipment that isn't a creature remain attached to it.")
        ruling("2022-02-18", "An Equipment creature with reconfigure can be attached to creatures by effects other than its reconfigure ability, such as the activated ability of Brass Squire.")
        ruling("2022-02-18", "Although it causes an Equipment to become attached to a creature, reconfigure is not an \"equip ability\" for the purpose of cards like Fighter Class and Leonin Shikari.")
        ruling("2022-02-18", "An Equipment creature can never become attached to itself. If an effect tries to do this, nothing happens.")
    }
}
