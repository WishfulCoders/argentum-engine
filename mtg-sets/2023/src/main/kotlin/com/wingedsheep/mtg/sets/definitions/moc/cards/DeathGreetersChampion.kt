package com.wingedsheep.mtg.sets.definitions.moc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Death-Greeter's Champion
 * {2}{R}
 * Creature — Human Warrior
 * 2/1
 * Dash {3}{R}
 * Backup 1
 * Double strike
 *
 * Fearless Skald's backup shape plus a dash cost. Dash is printed above backup, so backup confers
 * only double strike (the one ability printed below it) on another creature; the counter always lands.
 */
val DeathGreetersChampion = card("Death-Greeter's Champion") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "Dash {3}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's " +
        "returned from the battlefield to its owner's hand at the beginning of the next end step.)\n" +
        "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another " +
        "creature, it gains the following ability until end of turn.)\n" +
        "Double strike"
    power = 2
    toughness = 1

    dash = "{3}{R}"

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cb2b582-1c45-4bb2-8aef-59a71a5a9e94.jpg?1783917284"
    }
}
