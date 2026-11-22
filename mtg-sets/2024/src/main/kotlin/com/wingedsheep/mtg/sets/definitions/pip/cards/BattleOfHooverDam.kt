package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val BattleOfHooverDam = card("Battle of Hoover Dam") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose NCR or Legion.\n• NCR — At the beginning of your end step, return target creature card with mana value 3 or less from your graveyard to the battlefield with a finality counter on it.\n• Legion — Whenever a creature you control dies, put two +1/+1 counters on target creature you control."

    replacementEffect(EntersWithChoice(
        choiceType = ChoiceType.MODE,
        modeOptions = listOf(
            ModeOption(id = "ncr", label = "NCR", description = "Return a creature with mana value 3 or less at your end step."),
            ModeOption(id = "legion", label = "Legion", description = "When your creature dies, put two +1/+1 counters on your creature.")
        )
    ))
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        triggerRestriction = SourceChosenModeIs("ncr")
        val creature = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(3))
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD,
            addCounterType = CounterType.FINALITY)
    }
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        triggerRestriction = SourceChosenModeIs("legion")
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59c0f13a-4e83-4e0b-a790-1f78b5ee6a64.jpg?1783912419"
        ruling("2024-03-08", "Finality counters aren’t keyword counters, and a finality counter doesn’t give any abilities to the permanent it’s on. If that permanent loses its abilities and then would go to a graveyard, it will still be exiled instead.")
        ruling("2024-03-08", "Finality counters work on any permanent, not only creatures. If a permanent with a finality counter on it would be put into a graveyard from the battlefield, exile it instead.")
        ruling("2024-03-08", "Finality counters don’t stop permanents from going to zones other than the graveyard from the battlefield. For example, if a permanent with a finality counter on it would be put into its owner’s hand from the battlefield, it does so normally.")
        ruling("2024-03-08", "Multiple finality counters on a single permanent are redundant.")
        ruling("2024-03-08", "If you somehow control Battle of Hoover Dam and no choice was made for it (perhaps because another permanent on the battlefield became a copy of it), it has neither of the two triggered abilities.")
    }
}
