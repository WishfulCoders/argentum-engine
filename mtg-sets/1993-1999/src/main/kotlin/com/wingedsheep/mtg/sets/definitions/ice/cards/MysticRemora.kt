package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.cumulativeUpkeep
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mystic Remora — Ice Age #87 (canonical printing)
 * {U} · Enchantment
 *
 * Cumulative upkeep {1}
 * Whenever an opponent casts a noncreature spell, you may draw a card unless that player pays {4}.
 *
 *  - Cumulative upkeep is the [cumulativeUpkeep] builder (CR 702.24): an age counter each upkeep,
 *    then pay {1} per age counter or sacrifice it — all or nothing (ruling).
 *  - The draw is Rhystic Study's shape narrowed to noncreature spells: the caster is asked to pay
 *    {4} first, and only an unpaid toll reaches the Remora's controller's "you may draw".
 */
val MysticRemora = card("Mystic Remora") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Cumulative upkeep {1} (At the beginning of your upkeep, put an age counter on this " +
        "permanent, then sacrifice it unless you pay its upkeep cost for each age counter on it.)\n" +
        "Whenever an opponent casts a noncreature spell, you may draw a card unless that player pays {4}."

    cumulativeUpkeep(ManaCost.parse("{1}"))

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Noncreature)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana("{4}"),
            suffer = Effects.May(Effects.DrawCards(1)),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            consequenceDescription = "let Mystic Remora's controller draw a card",
        )
        description = "Whenever an opponent casts a noncreature spell, you may draw a card unless that player pays {4}."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Ken Meyer, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58e93dff-b774-4765-b7bd-d3957e42ff4a.jpg?1783947511"
        ruling("2025-10-02", "Paying cumulative upkeep is always optional. If it's not paid, the permanent with cumulative upkeep is sacrificed. Partial payments of the total cumulative upkeep cost can't be made. For example, if Mystic Remora has three age counters on it when its cumulative upkeep ability triggers, it gets another age counter and then its controller chooses to either pay {4} or sacrifice it.")
        ruling("2025-10-02", "Cumulative upkeep is a triggered ability that imposes an increasing cost on a permanent. \"Cumulative upkeep [cost]\" means \"At the beginning of your upkeep, if this permanent is on the battlefield, put an age counter on this permanent. Then you may pay [cost] for each age counter on it. If you don't, sacrifice it.\"")
    }
}
