package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Magmatic Galleon
 * {3}{R}{R}
 * Artifact — Vehicle
 * 5/5
 *
 * When this Vehicle enters, it deals 5 damage to target creature an opponent controls.
 * Whenever one or more creatures your opponents control are dealt excess noncombat damage,
 * create a Treasure token.
 * Crew 2
 *
 * The excess-noncombat-damage payoff reuses the Gap 12 excess-damage trigger primitive
 * (`Triggers.<subject>.dealsDamage(to, damageType, requireExcess, batch, requires)`, the same primitive Fall of Cair Andros composes) with `batch = true`
 * for the printed "one or more creatures" wording (CR 603.2c) — a sweeper dealing excess damage
 * to several opposing creatures simultaneously makes one Treasure, not one per creature —
 * and pairs it with `Effects.CreateTreasure()`. The Galleon's own ETB 5-damage strike is a
 * common source of that excess damage.
 */
val MagmaticGalleon = card("Magmatic Galleon") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "When this Vehicle enters, it deals 5 damage to target creature an opponent controls.\n" +
        "Whenever one or more creatures your opponents control are dealt excess noncombat damage, create a Treasure token.\n" +
        "Crew 2"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(5, creature)
    }

    triggeredAbility {
        trigger = Triggers.a().dealsDamage(Recipient.CreatureOpponentControls, damageType = DamageType.NonCombat, requireExcess = true, batch = true)
        effect = Effects.CreateTreasure()
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4471a833-11b9-4146-a9c0-84a6896c94d8.jpg?1782694483"
        ruling("2023-11-10", "Excess damage has been dealt to a creature if the damage dealt to it is greater than lethal damage. Usually, this means damage greater than its toughness, although damage already marked on the creature is taken into account.")
    }
}
