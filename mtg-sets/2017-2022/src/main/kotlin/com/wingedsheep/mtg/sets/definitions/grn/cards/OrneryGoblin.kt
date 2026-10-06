package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val OrneryGoblin = card("Ornery Goblin") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin Warrior"
    oracleText = "Whenever this creature blocks or becomes blocked by a creature, this creature deals 1 damage to that creature."
    colorIdentity = "R"
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.blocksOrBecomesBlocked(GameObjectFilter.Creature)
        effect = Effects.DealDamage(1, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Johann Bodin"
        flavorText = "\"Nobody ever listens to my complaints! Not even when I use the listening stick.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be1c1353-b315-4a0e-80b5-0d9a2962e35f.jpg?1783934159"
        ruling("2018-10-05", "The triggered ability triggers once for each creature blocking or blocked by Ornery Goblin. The ability resolves and deals damage to that creature before combat damage is dealt. If that damage destroys all creatures blocking Ornery Goblin, Ornery Goblin doesn’t become unblocked.")
    }
}
