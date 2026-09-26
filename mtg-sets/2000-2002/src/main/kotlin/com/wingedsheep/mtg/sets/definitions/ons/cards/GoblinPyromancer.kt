package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.core.Step
/**
 * Goblin Pyromancer
 * {3}{R}
 * Creature — Goblin Wizard
 * 2/2
 * When Goblin Pyromancer enters the battlefield, Goblin creatures get +3/+0 until end of turn.
 * At the beginning of the end step, destroy all Goblins.
 */
val GoblinPyromancer = card("Goblin Pyromancer") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Wizard"
    power = 2
    toughness = 2
    oracleText = "When Goblin Pyromancer enters the battlefield, Goblin creatures get +3/+0 until end of turn.\nAt the beginning of the end step, destroy all Goblins."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.allCreaturesWithSubtype("Goblin"),
            effect = Effects.ModifyStats(3, 0, EffectTarget.IterationEntity)
        )
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.allPermanentsWithSubtype("Goblin"),
            effect = Effects.Move(EffectTarget.IterationEntity, Zone.GRAVEYARD, byDestruction = true)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"The good news is, we figured out how the wand works. The bad news is, we figured out how the wand works.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb4815b7-fc20-44a4-ad1c-66d92993557f.jpg?1562939185"
    }
}
