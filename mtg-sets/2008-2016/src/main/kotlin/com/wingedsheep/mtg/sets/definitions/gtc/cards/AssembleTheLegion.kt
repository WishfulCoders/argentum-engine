package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Assemble the Legion
 * {3}{R}{W}
 * Enchantment
 *
 * At the beginning of your upkeep, put a muster counter on this enchantment. Then create a 1/1
 * red and white Soldier creature token with haste for each muster counter on this enchantment.
 *
 * The token count is `countersOnSelf(MUSTER)` read at resolution, after the counter lands (the
 * Anim Pakal shape). `EffectTarget.Self` is a LIVE_THEN_LKI reference, so if the enchantment has
 * left the battlefield by then the count comes from its last-known counters (2024-01-12 ruling).
 */
val AssembleTheLegion = card("Assemble the Legion") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, put a muster counter on this enchantment. Then create a 1/1 red and white Soldier creature token with haste for each muster counter on this enchantment."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.MUSTER, 1, EffectTarget.Self) then
            Effects.CreateToken(
                count = DynamicAmounts.countersOnSelf(CounterType.MUSTER),
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED, Color.WHITE),
                creatureTypes = setOf("Soldier"),
                keywords = setOf(Keyword.HASTE),
                imageUri = "https://cards.scryfall.io/normal/front/a/8/a8a5a5a5-a8ae-41ed-9af5-b99d80ce0b35.jpg?1783940147",
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43675ed7-ece1-4414-965e-9ebadcbf3dfb.jpg?1783940113"
        ruling("2024-01-12", "If Assemble the Legion isn't on the battlefield when its ability resolves, use the number of muster counters it had when it was last on the battlefield to determine how many Soldier tokens to create.")
    }
}
