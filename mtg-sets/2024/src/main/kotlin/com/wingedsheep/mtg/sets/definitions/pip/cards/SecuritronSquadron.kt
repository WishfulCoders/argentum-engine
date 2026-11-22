package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mechanics.squad
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Securitron Squadron — Fallout #23.
 * {1}{W} · Artifact Creature — Robot · 2/2
 *
 * Squad {3}
 * Vigilance
 * Whenever a creature token you control enters, put a +1/+1 counter on it.
 *
 * The counter trigger is ANY-bound (`Triggers.a`), so a Securitron Squadron that is itself a token
 * triggers on its own entry, and each token copy from squad sees every token entering alongside
 * it — the ruling's three-copy example ends with four counters on each copy.
 */
val SecuritronSquadron = card("Securitron Squadron") {
    manaCost = "{1}{W}"
    typeLine = "Artifact Creature — Robot"
    power = 2
    toughness = 2
    oracleText = "Squad {3} (As an additional cost to cast this spell, you may pay {3} any number of times. When this creature enters, create that many tokens that are copies of it.)\n" +
        "Vigilance\n" +
        "Whenever a creature token you control enters, put a +1/+1 counter on it."

    squad("{3}")
    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.token().youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        description = "Whenever a creature token you control enters, put a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b689a206-aec3-4a31-95cf-3d4b840db04c.jpg?1783912416"
        ruling("2024-03-08", "You may pay the squad cost any number of times. You will create a token that is a copy of that permanent for each time you paid the squad cost. If you paid the squad cost multiple times, the tokens will all enter the battlefield simultaneously.")
        ruling("2024-03-08", "If Securitron Squadron enters the battlefield at the same time as one or more creature tokens, its last ability will trigger for each of those creature tokens. If that Securitron Squadron is itself a creature token, its last ability will trigger when it enters the battlefield as well.")
        ruling("2024-03-08", "For example, if you pay Securitron Squadron's squad cost three times, then you'll create three token copies of Securitron Squadron when its squad ability resolves. Each of those tokens will enter the battlefield simultaneously. When they enter, their last abilities will each trigger three times (once for each of the other two tokens, and once for itself.) If the original Securitron Squadron is still on the battlefield, its last ability will trigger three times as well. After all of the triggered abilities resolve, each of the three token copies of Securitron Squadron will have four +1/+1 counters on it.")
        ruling("2024-03-08", "If the spell resolves but the creature with squad leaves the battlefield before its squad ability resolves, you'll still create the token copies.")
        ruling("2024-03-08", "If the spell is countered, the squad ability will not trigger, and no tokens will be created.")
    }
}
