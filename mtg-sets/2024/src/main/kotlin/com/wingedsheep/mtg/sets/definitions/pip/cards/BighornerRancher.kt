package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bighorner Rancher — Fallout #73.
 *
 * The mana ability scales with a MAX-power [DynamicAmounts.battlefield] aggregate over creatures you
 * control (the Rancher included), read off projected state; 0 or less adds no mana (ruling). The
 * sacrifice ability aggregates MAX toughness over *other* creatures (`excludeSelf = true`, the
 * Flourishing Hunter idiom) at resolution — the Rancher is already gone as the sacrifice cost —
 * and 0 or less gains no life (ruling).
 */
val BighornerRancher = card("Bighorner Rancher") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Ranger"
    power = 2
    toughness = 5
    oracleText = "Vigilance\n" +
        "{T}: Add an amount of {G} equal to the greatest power among creatures you control.\n" +
        "Sacrifice this creature: You gain life equal to the greatest toughness among other creatures you control."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.GREEN,
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower(),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature,
                excludeSelf = true,
            ).maxToughness()
        )
        description = "You gain life equal to the greatest toughness among other creatures you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/861361bc-b357-4ff8-931b-a48d08817191.jpg?1783912401"
        ruling("2024-03-08", "Bighorner Rancher's second ability is a mana ability. It doesn't use the stack and can't be responded to. If the greatest power among creatures you control is somehow 0 or less, no mana is added.")
        ruling("2024-03-08", "The amount of life you gain from Bighorner Rancher's last ability is determined when the ability resolves. If at that time the greatest toughness among creatures you control is 0 or less (probably because all of your other creatures were destroyed or otherwise left the battlefield in response), you won't gain any life.")
    }
}
