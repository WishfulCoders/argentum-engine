package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val GoblinArsonist = card("Goblin Arsonist") {
    manaCost = "{R}"
    typeLine = "Creature — Goblin Shaman"
    oracleText = "When this creature dies, you may have it deal 1 damage to any target."
    colorIdentity = "R"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.dies()
        val victim = target(Targets.Any)
        optional = true
        effect = Effects.DealDamage(1, victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Wayne Reynolds"
        flavorText = "With great power comes great risk of getting yourself killed."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/707d396d-950b-4ab8-9db2-f40c8f7db062.jpg?1783941975"
    }
}
