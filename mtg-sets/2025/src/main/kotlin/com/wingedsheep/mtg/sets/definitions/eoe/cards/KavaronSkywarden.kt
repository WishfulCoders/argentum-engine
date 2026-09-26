package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Kavaron Skywarden
 * {4}{R}
 * Creature — Kavu Soldier
 * 4/5
 * Reach
 * Void — At the beginning of your end step, if a nonland permanent left the battlefield this turn
 *   or a spell was warped this turn, put a +1/+1 counter on this creature.
 */
val KavaronSkywarden = card("Kavaron Skywarden") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kavu Soldier"
    power = 4
    toughness = 5
    oracleText = "Reach\nVoid — At the beginning of your end step, if a nonland permanent left the battlefield this turn or a spell was warped this turn, put a +1/+1 counter on this creature."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Void
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "At the beginning of your end step, if a nonland permanent left the battlefield this turn or a spell was warped this turn, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Diana Franco"
        flavorText = "Scavengers looking to steal from Kav mining sites rarely make that mistake again."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/617038a8-0544-4d0e-8ff1-c786e60ecd59.jpg?1752947119"
    }
}
