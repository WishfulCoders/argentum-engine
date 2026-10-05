package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Glorybringer
 * {3}{R}{R} — Creature — Dragon 4/4 (Rare) — Amonkhet #134
 * Artist: Sam Burley
 *
 * Flying, haste
 * You may exert this creature as it attacks. When you do, it deals 4 damage to target non-Dragon
 * creature an opponent controls. (An exerted creature won't untap during your next untap step.)
 *
 * The optional exert is the [ExertAsItAttacks] static; "When you do" is its linked
 * `Triggers.self.exertedAsItAttacks()` reflexive trigger (as on Hydra Trainer). Glorybringer can be
 * exerted even with no legal target for the trigger (ruling 2017-04-18).
 */
val Glorybringer = card("Glorybringer") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying, haste\n" +
        "You may exert this creature as it attacks. When you do, it deals 4 damage to target non-Dragon " +
        "creature an opponent controls. (An exerted creature won't untap during your next untap step.)"

    keywords(Keyword.FLYING, Keyword.HASTE)

    staticAbility {
        ability = ExertAsItAttacks
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        val victim = target(TargetFilter.Creature.notSubtype(Subtype("Dragon")).opponentControls())
        effect = Effects.DealDamage(4, victim)
        description = "When you exert Glorybringer, it deals 4 damage to target non-Dragon creature an opponent controls."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Sam Burley"
        flavorText = "What the initiates face in the final trial is completely at Hazoret's discretion."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3277ad99-5682-4baa-b106-de15721876a6.jpg?1783936489"
        ruling("2017-04-18", "If a creature has a targeted triggered ability that triggers when you exert it, you can exert it even if there isn't a legal target for that triggered ability.")
        ruling("2017-04-18", "You exert a creature as you declare it as an attacking creature. You can't do so later in combat, and creatures put onto the battlefield attacking can't be exerted.")
    }
}
