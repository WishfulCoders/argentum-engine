package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kellan Joins Up
 * {G}{W}{U}
 * Legendary Enchantment
 *
 * When Kellan Joins Up enters, you may exile a nonland card with mana value 3 or less from
 * your hand. If you do, it becomes plotted.
 * Whenever a legendary creature you control enters, put a +1/+1 counter on each creature you
 * control.
 *
 * Part of the OTJ "Joins Up" cycle of Legendary Enchantments. The ETB is the same
 * gather → choose-up-to → exile → plot pipeline used by Make Your Own Luck, but sourced from
 * hand and filtered to nonland cards with mana value ≤ 3. `ChooseUpTo(1)` is the optional
 * "you may exile" fork; [MakePlottedEffect] (CR 718) no-ops on an empty selection, so declining
 * is safe. The legendary-enters trigger distributes a +1/+1 counter over every creature you
 * control via `ForEachInGroup` + `AddCounters(IterationEntity)`.
 */
val KellanJoinsUp = card("Kellan Joins Up") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GUW"
    typeLine = "Legendary Enchantment"
    oracleText = "When Kellan Joins Up enters, you may exile a nonland card with mana value 3 or less from your hand. If you do, it becomes plotted. (You may cast it as a sorcery on a later turn without paying its mana cost.)\n" +
        "Whenever a legendary creature you control enters, put a +1/+1 counter on each creature you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val kjuHand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val kjuToPlot = chooseUpTo(
                1,
                from = kjuHand,
                filter = GameObjectFilter.Nonland.manaValueAtMost(3),
                selectedLabel = "Exile and plot"
            )
            exile(kjuToPlot)
            run(Effects.MakePlotted(from = kjuToPlot))
        }
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.legendary().youControl()).enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Wylie Beckert"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e7f95d5-b279-4469-9c89-1e02630d61e6.jpg?1712356126"
    }
}
