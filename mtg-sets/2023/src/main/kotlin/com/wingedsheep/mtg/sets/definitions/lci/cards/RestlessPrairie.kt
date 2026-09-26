package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Restless Prairie
 * Land
 *
 * This land enters tapped.
 * {T}: Add {G} or {W}.
 * {2}{G}{W}: This land becomes a 3/3 green and white Llama creature until end of turn. It's still a land.
 * Whenever this land attacks, other creatures you control get +1/+1 until end of turn.
 */
val RestlessPrairie = card("Restless Prairie") {
    typeLine = "Land"
    colorIdentity = "GW"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {G} or {W}.\n" +
        "{2}{G}{W}: This land becomes a 3/3 green and white Llama creature until end of turn. It's still a land.\n" +
        "Whenever this land attacks, other creatures you control get +1/+1 until end of turn."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}{W}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Llama"),
            colors = setOf(Color.GREEN.name, Color.WHITE.name),
            duration = Duration.EndOfTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.OtherCreaturesYouControl,
            effect = Effects.ModifyStats(
                power = 1,
                toughness = 1,
                target = EffectTarget.IterationEntity,
                duration = Duration.EndOfTurn,
            ),
        )
        description = "Whenever this land attacks, other creatures you control get +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "281"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f94ef116-6aff-4f53-a7f9-be5e21c7afa4.jpg?1782694388"
    }
}
