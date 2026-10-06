package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Intrepid Adversary — Innistrad: Midnight Hunt #25
 * {1}{W} · Creature — Human Scout · Mythic · 3/1
 *
 * Lifelink
 * When this creature enters, you may pay {1}{W} any number of times. When you pay this cost one or
 * more times, put that many valor counters on this creature.
 * Creatures you control get +1/+1 for each valor counter on this creature.
 *
 * Modeling notes:
 *  - The enters trigger resolves with no choices; on resolution the controller may pay {1}{W} any
 *    number of times ([Effects.PayRepeatedly] with no cap — the offered count is capped only by what
 *    they can afford). Paying at least once fires the CR 603.12 reflexive ability **once**, however
 *    many times the cost was paid (CR 603.12a), and players may respond to it before the counters
 *    arrive (the ruling). Declining, or being unable to pay even once, fires nothing.
 *  - "That many" is [DynamicAmounts.timesPaid], carried from the payment into the reflexive
 *    ability's resolution.
 *  - The anthem is a layer-7c [GrantDynamicStats] over every creature you control — this one
 *    included — sized by the live valor count on this creature ([CounterType.VALOR]), so counters
 *    added or removed later change it immediately, and it ends when the creature leaves.
 */
val IntrepidAdversary = card("Intrepid Adversary") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout"
    power = 3
    toughness = 1
    oracleText = "Lifelink\n" +
        "When this creature enters, you may pay {1}{W} any number of times. When you pay this cost " +
        "one or more times, put that many valor counters on this creature.\n" +
        "Creatures you control get +1/+1 for each valor counter on this creature."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayRepeatedly("{1}{W}"),
            optional = true,
            reflexiveEffect = Effects.AddDynamicCounters(
                CounterType.VALOR,
                DynamicAmounts.timesPaid(),
                EffectTarget.Self,
            ),
            descriptionOverride = "You may pay {1}{W} any number of times. When you pay this cost " +
                "one or more times, put that many valor counters on this creature."
        )
        description = "When this creature enters, you may pay {1}{W} any number of times. When you " +
            "pay this cost one or more times, put that many valor counters on this creature."
    }

    staticAbility {
        val valor = DynamicAmounts.countersOnSelf(CounterType.VALOR)
        ability = GrantDynamicStats(
            filter = GroupFilter.AllCreaturesYouControl,
            powerBonus = valor,
            toughnessBonus = valor,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "25"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/773199f9-c83b-4a77-8342-9f1552ecf595.jpg?1783925657"
        ruling(
            "2021-09-24",
            "After you pay the {1}{W} cost one or more times, a second ability triggers and players " +
                "may respond to it. As that ability resolves, the Intrepid Adversary gets its valor counters."
        )
    }
}
