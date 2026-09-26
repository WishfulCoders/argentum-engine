package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceDamageWithCounters
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Force Bubble
 * {2}{W}{W}
 * Enchantment
 * If damage would be dealt to you, put that many depletion counters on
 * Force Bubble instead.
 * When there are four or more depletion counters on Force Bubble, sacrifice it.
 * At the beginning of each end step, remove all depletion counters from
 * Force Bubble.
 */
val ForceBubble = card("Force Bubble") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "If damage would be dealt to you, put that many depletion counters on Force Bubble instead.\nWhen there are four or more depletion counters on Force Bubble, sacrifice it.\nAt the beginning of each end step, remove all depletion counters from Force Bubble."

    // Replacement effect: damage to you → depletion counters on this enchantment
    // Sacrifice threshold handles the state-triggered "when 4+ counters, sacrifice" ability
    replacementEffect(
        ReplaceDamageWithCounters(
            counterType = CounterType.DEPLETION,
            sacrificeThreshold = 4,
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.You)
        )
    )

    // At the beginning of each end step, remove all depletion counters
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Effects.RemoveCounters(CounterType.DEPLETION, Int.MAX_VALUE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/742ac116-86ed-4ce6-9805-76f47a41c4c4.jpg?1562530652"
        ruling("2004-10-04", "If you control more than one, each time you take damage you can decide which one replaces the damage, and that one replaces all of the damage.")
        ruling("2004-10-04", "If you take 6 damage at once, it will replace all 6 damage and put 6 counters on it. Then the second ability will trigger.")
        ruling("2004-10-04", "The effect does not prevent damage, it replaces it so the damage is neither dealt or prevented.")
    }
}
