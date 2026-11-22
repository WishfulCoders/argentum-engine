package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * White Plume Adventurer
 * {2}{W}
 * Creature — Orc Cleric
 * 3/3
 * When this creature enters, you take the initiative.
 * At the beginning of each opponent's upkeep, untap a creature you control. If you've completed a
 * dungeon, untap all creatures you control instead.
 *
 * "Untap a creature you control" isn't targeted — the creature is chosen on resolution, so it is a
 * choose-one pipeline rather than a target.
 */
val WhitePlumeAdventurer = card("White Plume Adventurer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Orc Cleric"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you take the initiative.\n" +
        "At the beginning of each opponent's upkeep, untap a creature you control. If you've completed " +
        "a dungeon, untap all creatures you control instead."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.TakeTheInitiative()
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            Conditions.YouCompletedADungeon,
            Patterns.Group.untapGroup(GroupFilter.AllCreaturesYouControl),
            Effects.Pipeline {
                val creatures = gather(GameObjectFilter.Creature, player = Player.You)
                val chosen = chooseExactly(1, from = creatures, prompt = "Untap a creature you control")
                run(Effects.TapCollection(chosen, tap = false))
            }
        )
        description = "At the beginning of each opponent's upkeep, untap a creature you control. If " +
            "you've completed a dungeon, untap all creatures you control instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Joseph Weston"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b256ddc8-8b12-434c-a610-1a872e948f2f.jpg?1783922800"
        ruling("2022-06-10", "A player who currently has the initiative may take the initiative again. This causes that player to venture into Undercity again, but does not cause them to have multiple initiative designations.")
        ruling("2022-06-10", "You cannot venture into Undercity unless instructed to do so, either because you have the initiative at the beginning of your upkeep or because you take the initiative.")
        ruling("2022-06-10", "If you're already in a dungeon when instructed to venture into Undercity, you move to the next room of that dungeon. If you are already in the last room, you will complete that dungeon and start Undercity.")
    }
}
